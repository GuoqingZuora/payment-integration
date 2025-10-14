package com.zuora.settlement.integration.service;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.zuora.settlement.transaction.service.ARCommonUtil.max;
import static java.util.Optional.ofNullable;

import com.zuora.admin.permission.ApplicationPermission;
import com.zuora.admin.session.SessionManager;
import com.zuora.api.util.ApiError;
import com.zuora.base.Decimal;
import com.zuora.base.UserVisibleException;
import com.zuora.base.ZuoraException;
import com.zuora.base.timezone.ZDate;
import com.zuora.enums.ARTransactionCreationSource;
import com.zuora.enums.PaymentPartType;
import com.zuora.enums.PaymentSourceType;
import com.zuora.enums.PaymentStatus;
import com.zuora.enums.RefundSourceType;
import com.zuora.enums.RefundStatus;
import com.zuora.enums.RefundType;
import com.zuora.infra.performance.PerformanceMetrics;
import com.zuora.infra.performance.PerformanceMetricsContext;
import com.zuora.settlement.metrics.SettlementMetricsHelper;
import com.zuora.settlement.payment.service.PaymentConvertionInternalService;
import com.zuora.settlement.payment.service.PaymentInternalService;
import com.zuora.settlement.payment.service.PaymentService;
import com.zuora.settlement.payment.service.PaymentValidator;
import com.zuora.settlement.payment.vo.GETPaymentVO;
import com.zuora.settlement.payment.vo.PaymentApplicationRequestVO;
import com.zuora.settlement.payment.vo.PaymentApplyParameter;
import com.zuora.settlement.payment.vo.PaymentCreationParameter;
import com.zuora.settlement.paymentschedule.service.PaymentScheduleItemService;
import com.zuora.settlement.refund.service.RefundService;
import com.zuora.settlement.refund.vo.RefundCreationVO;
import com.zuora.settlement.refund.vo.RefundQueryVO;
import com.zuora.settlement.transaction.allocation.rules.CreateRefundToLegacyPaymentFIFORule;
import com.zuora.settlement.transaction.allocation.rules.UnapplyPaymentFromRelatedInvoiceFIFORule;
import com.zuora.settlement.transaction.service.ARCommonUtil;
import com.zuora.settlement.transaction.service.ARTransactionHelper;
import com.zuora.settlement.transaction.service.ARTransactionPartInternalService;
import com.zuora.settlement.transaction.service.impl.ARTransactionPartInternalServiceImpl;
import com.zuora.settlement.utils.RefundConvertUtil;
import com.zuora.util.properties.PropertiesService;
import com.zuora.zbilling.account.business.BillingAccountManager;
import com.zuora.zbilling.account.model.BillingAccount;
import com.zuora.zbilling.exception.ValidateException;
import com.zuora.zbilling.invoice.business.InvoiceManager;
import com.zuora.zbilling.invoice.model.Invoice;
import com.zuora.zbilling.invoice.util.BusinessLogUtil;
import com.zuora.zbilling.memo.model.Memo;
import com.zuora.zbilling.payment.business.ElectronicPaymentService;
import com.zuora.zbilling.payment.business.PaymentInvoiceManager;
import com.zuora.zbilling.payment.business.PaymentManager;
import com.zuora.zbilling.payment.model.Payment;
import com.zuora.zbilling.payment.model.PaymentInvoice;
import com.zuora.zbilling.payment.model.PaymentPart;
import com.zuora.zbilling.paymentgateway.gateway.CCTransactionResult;
import com.zuora.zbilling.paymentgateway.gateway.CCTransactionResultCode;
import com.zuora.zbilling.paymentmethod.business.PaymentMethodManager;
import com.zuora.zbilling.paymentrun.business.PaymentRunManager;
import com.zuora.zbilling.paymentrun.business.PaymentRunPaymentQueueLogManager;
import com.zuora.zbilling.refund.business.refund.RefundManager;
import com.zuora.zbilling.refund.business.refund.RefundServiceFacade;
import com.zuora.zbilling.refund.model.Refund;
import com.zuora.zbilling.refund.model.RefundPaymentInvoice;
import com.zuora.zbilling.refund.util.RefundUtil;
import com.zuora.zbilling.setting.business.CurrencyManager;
import com.zuora.zbilling.setting.customfield.CustomFieldService;
import com.zuora.zbilling.setting.gateway.business.PaymentGatewayManager;
import com.zuora.zbilling.setting.paymentrules.business.PaymentRulesManager;
import com.zuora.zbilling.setting.paymentrules.model.PaymentRules;

import com.kenoah.base.locale.Localizer;
import com.kenoah.base.util.BeanUtil;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.sql.Date;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Resource;

@Service("ARGatewayIntegrationService")
public class ARGatewayIntegrationServiceImpl implements ARGatewayIntegrationService {

    protected static final String NON_AR_PAYMENT_CURRENCY_VALIDATION = "nonAR.payment.currency.validation";

    protected static final String GW_OPTIONS_PASS_DISABLE = "gw.options.pass.disable";

    @Autowired
    private Localizer localizer;
    @Autowired
    private SessionManager sessionManager;
    @Autowired
    private PaymentManager paymentManager;
    @Autowired
    private PerformanceMetrics performanceMetrics;
    @Autowired
    @Qualifier("paymentRunPaymentQueueLogManager")
    private PaymentRunPaymentQueueLogManager prqMgr;
    @Resource(name = "RefundService")
    private RefundService refundService;
    @Resource(name = "paymentServiceAR")
    private PaymentService paymentService;
    @Autowired
    private UnapplyPaymentFromRelatedInvoiceFIFORule unapplyPaymentFromRelatedInvoiceFIFORule;
    @Autowired
    private CreateRefundToLegacyPaymentFIFORule createRefundToLegacyPaymentFIFORule;
    @Autowired
    private RefundManager refundManager;
    @Autowired
    private RefundServiceFacade refundServiceFacade;
    @Resource(name = "PaymentConvertionInternalService")
    private PaymentConvertionInternalService paymentConvertionInternalService;
    @Autowired
    private PaymentMethodManager paymentMethodManager;
    @Autowired
    private PaymentRunManager paymentRunManager;
    @Autowired
    private SettlementMetricsHelper settlementMetricsHelper;
    @Resource(name = ARTransactionPartInternalServiceImpl.BEAN_NAME)
    private ARTransactionPartInternalService partService;
    @Autowired
    private PaymentInvoiceManager paymentInvoiceManager;
    @Autowired
    private ElectronicPaymentService electronicPaymentService;
    @Autowired
    private BillingAccountManager billingAccountManager;
    @Autowired
    private InvoiceManager invoiceManager;
    @Autowired
    private PaymentGatewayManager paymentGatewayManager;
    @Autowired
    private CurrencyManager currencyManager;
    @Resource(name = PaymentInternalService.BEAN_NAME)
    private PaymentInternalService paymentInternalService;
    @Autowired
    private SessionFactory knSessionFactory;
    @Autowired
    private CustomFieldService customFieldService;
    @Autowired
    private PaymentRulesManager paymentRulesManager;
    @Autowired
    private PropertiesService propertiesService;
    @Resource(name = "PaymentValidator")
    private PaymentValidator validator;
    @Resource(name = PaymentScheduleItemService.BEAN_NAME)
    private PaymentScheduleItemService paymentScheduleItemService;

    @Autowired
    private ARTransactionStatusService arTransactionStatusService;

    /**
     * TODO call back from gateway.
     * SET-1695, SET-1789 High volume payment run POC.
     * <p>
     * Make sure your transaction and hibernate session is commit.
     * Please do not change the global session like FlushMode.
     * The FlushMode should be auto commit.
     *
     * @param paymentId
     * @param result
     * @throws UserVisibleException if payment object is null, or transactionResult  is null
     */
    @Override
    @Transactional
    public void updatePaymentStatusByTransactionResult(String paymentId, CCTransactionResult result) throws Exception {
        PerformanceMetricsContext pmc =
                performanceMetrics.start("PaymentRun: updatePaymentStatusByTransactionResult -- update payment with transactionResult, payment id=" + paymentId);
        if (result == null) {
            throw new UserVisibleException(localizer.getMessage("Payment.updateStatus.fail", paymentId));
        }
        Payment payment = paymentManager.getObject(paymentId);
        if (payment == null) {
            throw new UserVisibleException(localizer.getMessage("errors.body.DataNotAvailable"));
        }
        if (!CCTransactionResultCode.Unknown.equals(result.getCcTransactionResultCode())) {
            try {
                if (PaymentStatus.Processing.equals(payment.getPaymentStatus())) {
                    arTransactionStatusService.updatePaymentStatusByTransactionResult(payment, result);
                }
                settlementMetricsHelper.meterHighVolumePaymentRun(payment.getTenantId(), SettlementMetricsHelper.ACTION_DEQUEUE,
                        SettlementMetricsHelper.RESULT_SUCCESS);
            } catch (Exception e) {
                settlementMetricsHelper.meterHighVolumePaymentRun(payment.getTenantId(), SettlementMetricsHelper.ACTION_DEQUEUE,
                        SettlementMetricsHelper.RESULT_FAIL);
                throw e;
            }
        } else {
            settlementMetricsHelper.meterHighVolumePaymentRun(payment.getTenantId(), SettlementMetricsHelper.ACTION_DEQUEUE,
                    SettlementMetricsHelper.RESULT_UNKNOWN);
            BusinessLogUtil.writeBusinessLog("Update payment with transactionResult error with Unknown , payment id=" + paymentId,
                    "ARGatewayIntegrationService.updatePaymentStatusByTransactionResult");
        }
        updateOrSafeDeletePaymentDequeued(payment);
        pmc.stop();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public Payment fixStuckPaymentByTransactionResult(String paymentId, CCTransactionResult transactionResult) {
        checkArgument(StringUtils.isNotBlank(paymentId), "paymentId is blank.");
        checkArgument(transactionResult != null, "transactionResult is null.");
        checkArgument(transactionResult.getCcTransactionResultCode() != null &&
                        transactionResult.getCcTransactionResultCode() != CCTransactionResultCode.Unknown,
                "transactionResultCode is Unknown.");

        Payment payment = paymentManager.getObject(paymentId);
        checkArgument(payment != null,
                "the payment specified by [" + paymentId + "] is not found.");
        checkState(PaymentStatus.Processing.equals(payment.getPaymentStatus()),
                "the payment specified by [" + paymentId + "] is not Processing.");

        arTransactionStatusService.updatePaymentStatusByTransactionResult(payment, transactionResult);

        return payment;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void updateRefundStatusByTransactionResult(String refundId) {
        checkArgument(StringUtils.isNotBlank(refundId), "refundId is blank.");

        Refund refund = refundManager.getObject(refundId);
        checkArgument(refund != null,
                "the refund specified by [" + refundId + "] is not found.");
        checkState(RefundStatus.Processing.equals(refund.getStatus()),
                "the refund specified by [" + refundId + "] is not Processing.");

        arTransactionStatusService.updateRefundStatus(refund);
    }

    /*
     * if processing status - safe delete row. if processed - update.
     */
    private void updateOrSafeDeletePaymentDequeued(Payment payment) {
        if (payment != null && payment.getSourceName() != null && PaymentSourceType.PaymentRun.equals(payment.getSourceType())) {
            prqMgr.updatePaymentDequeuedLog(payment.getId());
        }
    }

    private void validateRefundForPaymentReconciliationVO(RefundForPaymentReconciliationVO refundForPaymentReconciliationVO) {
        if (refundForPaymentReconciliationVO.getPaymentId() == null) {
            throw new UserVisibleException(localizer.getMessage("AR.Refund.create.PaymentIdIsNull"));
        }
        if (BeanUtil.isBlank(refundForPaymentReconciliationVO.getAmount())) {
            throw new UserVisibleException(localizer.getMessage("Refund.validate.refundTotalAmountRequired"));
        }
        if (refundForPaymentReconciliationVO.getRefundDate() == null) {
            throw new UserVisibleException(localizer.getMessage("Refund.validate.refundDateRequired"));
        }
    }

    private Refund convertToRefundFrom(RefundForPaymentReconciliationVO refundForPaymentReconciliationVO, Payment payment, boolean isCBRefund) {
        Refund refund = refundManager.createNewObject();
        refund.setType(RefundType.External);
        RefundSourceType refundSourcetype = isCBRefund ? RefundSourceType.CreditBalance : RefundSourceType.Payment;
        refund.setSourceType(refundSourcetype);
        if (!isCBRefund) {
            refund.setPayment(payment);
        }
        refund.setStatus(RefundStatus.Processed);
        refund.setRefundTransactionType(refundForPaymentReconciliationVO.getRefundTransactionType());
        refund.setRequestSource(refundForPaymentReconciliationVO.getRequestSource());
        refund.setRefundDate(refundForPaymentReconciliationVO.getRefundDate());
        refund.setTotalAmount(refundForPaymentReconciliationVO.getAmount());
        refund.setGatewayState(refundForPaymentReconciliationVO.getGatewayState());
        refund.setSettledOn(refundForPaymentReconciliationVO.getSettledOn());
        refund.setPaymentMethod(paymentMethodManager.getObject(refundForPaymentReconciliationVO.getPaymentMethodId()));
        refund.setRefundMethodType(refundForPaymentReconciliationVO.getMethodType());
        refund.setBillingAccount(payment.getBillingAccount());
        refund.setReferenceID(refundForPaymentReconciliationVO.getReferenceId());
        refund.setSecondRefundReferenceId(refundForPaymentReconciliationVO.getSecondRefundReferenceId());
        refund.setReasonCode(refundForPaymentReconciliationVO.getReasonCode());
        refund.setGwTransactionResultNumber(refundForPaymentReconciliationVO.getGatewayReasonCode());
        refund.setGwTransactionResultMessage(refundForPaymentReconciliationVO.getGatewayReason());
        List<RefundPaymentInvoice> refundPaymentInvoices = (List<RefundPaymentInvoice>) createRefundToLegacyPaymentFIFORule.generate(refund, payment);
        refund.setRefundPaymentInvoices(refundPaymentInvoices);
        return refund;
    }

    private RefundCreationVO convertToRefundCreationVOFrom(RefundForPaymentReconciliationVO refundForPaymentReconciliationVO) {
        RefundCreationVO refundCreationVO = new RefundCreationVO();
        refundCreationVO.setRefundType(RefundType.External);
        refundCreationVO.setPaymentId(refundForPaymentReconciliationVO.getPaymentId());
        refundCreationVO.setRefundDate(refundForPaymentReconciliationVO.getRefundDate());
        refundCreationVO.setRefundAmount(refundForPaymentReconciliationVO.getAmount());
        refundCreationVO.setGatewayState(refundForPaymentReconciliationVO.getGatewayState());
        refundCreationVO.setSettledOn(refundForPaymentReconciliationVO.getSettledOn());
        refundCreationVO.setPaymentMethod(paymentMethodManager.getObject(refundForPaymentReconciliationVO.getPaymentMethodId()));
        refundCreationVO.setMethodType(refundForPaymentReconciliationVO.getMethodType());
        refundCreationVO.setReferenceId(refundForPaymentReconciliationVO.getReferenceId());
        refundCreationVO.setSecondRefundReferenceId(refundForPaymentReconciliationVO.getSecondRefundReferenceId());
        refundCreationVO.setReasonCode(refundForPaymentReconciliationVO.getReasonCode());
        refundCreationVO.setGwTransactionResultNumber(refundForPaymentReconciliationVO.getGatewayReasonCode());
        refundCreationVO.setGwTransactionResultMessage(refundForPaymentReconciliationVO.getGatewayReason());
        refundCreationVO.setRefundTransactionType(refundForPaymentReconciliationVO.getRefundTransactionType());
        refundCreationVO.setRequestSource(refundForPaymentReconciliationVO.getRequestSource());
        return refundCreationVO;
    }

    @Override
    public String createPayment(PaymentCreationParameter param) {
        if (ARCommonUtil.isARPermissionEnabled()) {
            return paymentService.create(param);
        } else {
            BillingAccount billingAccount = billingAccountManager.getBillingAccountById(param.getAccountId());

            Payment payment = paymentManager.createNewObject();
            payment.setBillingAccount(billingAccount);
            payment.setPaymentGateway(paymentGatewayManager.getPaymentGatewayById(param.getGatewayId()));

            Decimal paymentAmount = param.getTotalAmount();
            Decimal amountAppliedToInvoices = Decimal.ZERO;
            for (PaymentApplicationRequestVO pavo : param.getPaymentApplicationVOs()) {
                PaymentInvoice pi = paymentInvoiceManager.createNewObject();
                pi.setInvoice(invoiceManager.getById(pavo.getInvoiceId()));
                pi.setPayment(payment);
                pi.setApplyAmount(pavo.getApplyAmount());
                payment.getPaymentInvoices().add(pi);

                amountAppliedToInvoices = amountAppliedToInvoices.plus(pavo.getApplyAmount());
            }

            if (!propertiesService.getBoolean(GW_OPTIONS_PASS_DISABLE, false)) {
                if (!CollectionUtils.isEmpty(param.getGatewayOptions())) {
                    payment.setGatewayOptions(param.getGatewayOptions());
                }
            }

            payment.setPaymentAmount(paymentAmount);
            payment.setAvailableRefundAmount(amountAppliedToInvoices);

            payment.setCreditBalanceAmount(paymentAmount.minus(amountAppliedToInvoices));

            payment.setPaymentType(param.getPaymentType());
            payment.setSourceType(param.getPaymentSourceType());
            payment.setSourceName(param.getPaymentSourceName());
            payment.setPaymentMethod(paymentMethodManager.getObject(param.getPaymentMethodId()));
            payment.setPaymentEffectiveDate(param.getEffectiveDate());

            payment.setCurrency(currencyManager.getCurrencyById(param.getCurrencyId()));
            payment.setPaymentReferenceId(param.getReferenceId());
            payment.setAuthTransactionId(param.getAuthTransactionId());
            payment.setGatewayOrderId(param.getGatewayOrderId());
            boolean isConfigPropertyNonARValidationEnable =
                    (propertiesService.getBoolean(NON_AR_PAYMENT_CURRENCY_VALIDATION).isPresent() && propertiesService.getBoolean(NON_AR_PAYMENT_CURRENCY_VALIDATION).equals(Optional.of(true)));
            if (isConfigPropertyNonARValidationEnable) {
                validator.validateIfCurrencyMatchWithAccountForNonAR(payment.getCurrency(),billingAccount);
            }
            electronicPaymentService.processPayment(payment);

            return payment.getId();
        }
    }

    @Override
    @Transactional
    public String createRefundForPaymentReconciliation(RefundForPaymentReconciliationVO refundForPaymentReconciliationVO) throws ZuoraException {
        validateRefundForPaymentReconciliationVO(refundForPaymentReconciliationVO);
        Payment payment = paymentManager.get(refundForPaymentReconciliationVO.getPaymentId());
        Refund refundCf = new Refund();
        customFieldService.setDefaultCustomFieldToObject(refundCf);
        if (payment == null) {
            throw new ValidateException(ApiError.NOT_FOUND, localizer.getMessage("AR.Payment.validator.id.but.no.instance", refundForPaymentReconciliationVO.getPaymentId()));
        }
        return createRefundForPaymentReconciliationForAllPaymentSources(refundForPaymentReconciliationVO, payment, refundCf);
    }

    private String createRefundForPaymentReconciliationForAllPaymentSources(RefundForPaymentReconciliationVO refundForPaymentReconciliationVO, Payment payment, Refund refundCf) {
        boolean hasARSettlementPermission = sessionManager.getUserSession().hasPermission(ApplicationPermission.ARSettlement);
        boolean hasInvoiceSettlementHarmonizationPermission = sessionManager.getUserSession().hasPermission(ApplicationPermission.InvoiceSettlementHarmonization);
        boolean isInvoiceSettlementSource = ARTransactionCreationSource.InvoiceSettlement.equals(payment.getCreationSource());
        if (hasARSettlementPermission || (hasInvoiceSettlementHarmonizationPermission && isInvoiceSettlementSource)) {
            return createRefundFromPaymentForReconciliationWithInvoiceSettlementOn(refundForPaymentReconciliationVO, payment, refundCf);
        } else {
            return createRefundFromPaymentForReconciliationWithInvoiceSettlementOff(refundForPaymentReconciliationVO, payment, refundCf);
        }
    }

    private String createRefundFromPaymentForReconciliationWithInvoiceSettlementOn(RefundForPaymentReconciliationVO refundForPaymentReconciliationVO, Payment payment, Refund refundCf) {
        RefundCreationVO refundCreationVO = convertToRefundCreationVOFrom(refundForPaymentReconciliationVO);

        if (payment.isSettlementSupported()) { // the standalone payment can only be refunded with unapplied amount, no need to unapply.
            Decimal availableRefundAmount = RefundUtil.getAvailableAmountForRefundWithAllPaymentCreationSource(payment);
            if (availableRefundAmount.isLessThan(refundCreationVO.getRefundAmount())) {
                PaymentApplyParameter param = unapplyPaymentFromRelatedInvoiceFIFORule
                        .generate(payment, refundCreationVO.getRefundAmount().minus(availableRefundAmount), refundCreationVO.getRefundDate());
                paymentService.unapply(param);
                refundCreationVO.setRefundDate(param.getEffectiveDate());
                knSessionFactory.getCurrentSession().flush();
            } else {
                Date refundDate = ofNullable(refundForPaymentReconciliationVO.getRefundDate()).orElseGet(() -> ZDate.todayInUserTz().toSqlDate());
                Date latestDate = paymentInternalService.queryLatestEffectiveDate(refundForPaymentReconciliationVO.getPaymentId());
                refundCreationVO.setRefundDate(max(latestDate, refundDate));
            }
        }
        refundCreationVO.setCustomFieldsData(refundCf.getCustomFieldsData());
        String refundId = refundService.create(refundCreationVO);
        paymentScheduleItemService.markPSIAsErrorByPaymentId(payment.getId());
        return refundId;
    }

    private String createRefundFromPaymentForReconciliationWithInvoiceSettlementOff(RefundForPaymentReconciliationVO refundForPaymentReconciliationVO, Payment payment, Refund refundCf) {
        if (payment.getCreditBalanceAmount().isPositive() && isCBRefundInGRSettingEnabled()) {
            if (payment.getCreditBalanceAmount().isGreaterThanOrEqualTo(refundForPaymentReconciliationVO.getAmount()) &&
                    payment.getBillingAccount().getCreditBalance().isGreaterThanOrEqualTo(refundForPaymentReconciliationVO.getAmount())) {
                Refund refund = convertToRefundFrom(refundForPaymentReconciliationVO, payment, true);
                refund.setCustomFieldsData(refundCf.getCustomFieldsData());
                refundServiceFacade.processSaveRefund(refund);
                return refund.getId();
            } else {
                throw new ValidateException(ApiError.EXTENSION_ERROR,
                        localizer.getMessage("AR.Payment.validator.payment.insufficient.credit.balance", refundForPaymentReconciliationVO.getPaymentId()));
            }
        } else {
            Refund refund = convertToRefundFrom(refundForPaymentReconciliationVO, payment, false);
            refund.setCustomFieldsData(refundCf.getCustomFieldsData());
            refundServiceFacade.processSaveRefund(refund);
            return refund.getId();
        }
    }

    @Override
    public GETPaymentVO retrievePayment(String id) {
        if (id == null) {
            throw new UserVisibleException(localizer.getMessage("AR.Refund.create.PaymentIdIsNull"));
        }
        // get payment by key (id or number)
        Payment payment = paymentManager.getPaymentByIdOrNumber(id);
        if (payment == null) {
            throw new UserVisibleException(localizer.getMessage("AR.Payment.validator.id.but.cannot.find", id));
        }
        return paymentConvertionInternalService.convertToPaymentBasicVO(payment);
    }

    @Override
    public RefundQueryVO retrieveRefund(String id) {
        if (id == null) {
            throw new UserVisibleException(localizer.getMessage("AR.Refund.create.IdIsNotNull"));
        }
        Refund refund = refundManager.get(id);
        if (refund == null) {
            throw new UserVisibleException(localizer.getMessage("AR.Refund.validator.id.but.cannot.find", id));
        }
        return RefundConvertUtil.assembleRefund(refund);
    }

    @Override
    @Transactional
    public List<PayableDocument> getPayableDocuments(String paymentId) {
        if (StringUtils.isBlank(paymentId)) {
            throw new UserVisibleException(localizer.getMessage("AR.Refund.create.PaymentIdIsNull"));
        }

        Payment payment = paymentManager.get(paymentId);

        if (payment == null) {
            throw new UserVisibleException(localizer.getMessage("AR.Payment.validator.id.but.cannot.find", paymentId));
        }

        if (ARTransactionHelper.isARPermissionEnabled()) {
            return extractPayableDocumentsFromPaymentParts(partService.queryPaymentParts(paymentId));
        } else {
            return extractPayableDocumentsFromPaymentInvoices(payment.getPaymentInvoices());
        }
    }

    private List<PayableDocument> extractPayableDocumentsFromPaymentParts(List<PaymentPart> paymentParts) {
        return Optional.ofNullable(paymentParts)
                .map(pps -> pps.stream().map(pp -> extractPayableDocument(pp)).filter(pd -> pd != null).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    private List<PayableDocument> extractPayableDocumentsFromPaymentInvoices(List<PaymentInvoice> paymentInvoices) {
        return Optional.ofNullable(paymentInvoices)
                .map(pis -> pis.stream().map(pi -> extractPayableDocument(pi)).filter(pd -> pd != null).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    private PayableDocument extractPayableDocument(PaymentInvoice paymentInvoice) {
        return Optional.ofNullable(paymentInvoice).map(pi -> pi.getInvoice()).map(inv ->
                PayableDocument.newPayableDocument()
                        .id(inv.getId())
                        .reference(inv.getInvoiceNumber())
                        .type(PayableDocumentType.Invoice)
                        .amount(inv.getTotalamount())
                        .applyAmount(paymentInvoice.getApplyAmount())
                        .tax(inv.getTax())
                        .build()
        ).orElse(null);
    }

    private PayableDocument extractPayableDocument(PaymentPart paymentPart) {
        return Optional.ofNullable(paymentPart).map(pp -> {
            if (PaymentPartType.InvoicePayment.equals(pp.getType())) {
                return extractInvoiceFromPaymentPart(pp);
            } else if (PaymentPartType.DebitMemoPayment.equals(pp.getType())) {
                return extractDebitMemoFromPaymentPart(pp);
            } else {
                return null;
            }
        }).orElse(null);
    }

    private PayableDocument extractInvoiceFromPaymentPart(PaymentPart paymentPart) {
        Invoice invoice = paymentPart.getInvoice();

        PayableDocument doc = PayableDocument.newPayableDocument()
                .id(invoice.getId())
                .reference(invoice.getInvoiceNumber())
                .type(PayableDocumentType.Invoice)
                .amount(invoice.getTotalamount())
                .applyAmount(paymentPart.getAmount())
                .tax(invoice.getTax())
                .build();
        return doc;
    }

    private PayableDocument extractDebitMemoFromPaymentPart(PaymentPart paymentPart) {
        Memo debitMemo = paymentPart.getDebitMemo();

        PayableDocument doc = PayableDocument.newPayableDocument()
                .id(debitMemo.getId())
                .reference(debitMemo.getMemoNumber())
                .type(PayableDocumentType.DebitMemo)
                .amount(debitMemo.getTotalAmount())
                .applyAmount(paymentPart.getAmount())
                .tax(debitMemo.getTaxAmount())
                .build();
        return doc;
    }

    @Override
    public boolean isCBRefundInGRSettingEnabled() {
        boolean isCBRefundInGREnabled = false;
        if (!sessionManager.getUserSession().hasPermission(ApplicationPermission.ARSettlement)) {
            PaymentRules rules = paymentRulesManager.queryPaymentRules();
            if (rules != null && rules.isEnableCBRefundInGR() != null && rules.isEnableCBRefundInGR().booleanValue()) {
                isCBRefundInGREnabled = true;
            }
        }
        return isCBRefundInGREnabled;
    }
}
