package com.zuora.opg.test.common;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.zuora.billing.opg.test.json.util.JsonTestConfig;
import com.zuora.billing.opg.test.support.auth.AuthTokenProviderType;
import com.zuora.billing.opg.test.support.auth.SimpleAuthTokenProviderFactoryForLiveTest;
import com.zuora.billing.opg.test.support.connector.ConnectorType;
import com.zuora.billing.opg.test.support.connector.SimpleConnectorFactoryForLiveTest;
import com.zuora.billing.opg.test.support.engine.OpenPaymentGatewayBuilder;
import com.zuora.billing.opg.test.support.engine.PaymentGatewayMetadataServiceForTestImpl;
import com.zuora.billing.opg.test.support.setting.PaymentGatewaySettingRepositoryProxyImpl;
import com.zuora.billing.opg.test.verify.ConnectorHeadersVerifier;
import com.zuora.enums.PaymentGatewayType;
import com.zuora.opg.plugin.service.OPGPluginService;
import com.zuora.util.properties.PropertiesService;
import com.zuora.zbilling.payment.business.PaymentGatewayMetadataFieldMappingService;
import com.zuora.zbilling.payment.business.PaymentGatewaySettingRepository;
import com.zuora.zbilling.payment.business.impl.PaymentGatewayTelemetrySignal;
import com.zuora.zbilling.setting.gateway.business.PaymentGatewayManager;
import com.zuora.zbilling.setting.gateway.model.PaymentGateway;
import com.zuora.zbilling.setting.regions.business.TenantRegionService;
import com.zuora.zpayment.openpaymentgateway.engine.OpenPaymentGateway;
import com.zuora.zpayment.openpaymentgateway.engine.RequestResponseProcessor;
import com.zuora.zpayment.openpaymentgateway.engine.RequestResponseProcessorFactory;
import com.zuora.zpayment.openpaymentgateway.engine.auth.AuthToken;
import com.zuora.zpayment.openpaymentgateway.engine.auth.AuthTokenProvider;
import com.zuora.zpayment.openpaymentgateway.engine.auth.AuthTokenTelemetrySignalService;
import com.zuora.zpayment.openpaymentgateway.engine.connector.Connector;
import com.zuora.zpayment.openpaymentgateway.engine.fsm.OperationStateMachineImpl;
import com.zuora.zpayment.openpaymentgateway.engine.gatewayconfig.PaymentGatewayConfigurationHandler;
import com.zuora.zpayment.openpaymentgateway.engine.plugin.OPGPluginLoader;
import com.zuora.zpayment.openpaymentgateway.engine.templateengine.ZUtility;
import com.zuora.zpayment.openpaymentgateway.model.PaymentGatewayAttribute;
import com.zuora.zpayment.openpaymentgateway.service.PaymentGatewayTransactionLogService;
import com.zuora.zpayment.openpaymentgateway.service.PaymentMethodLogService;

import com.google.common.collect.ImmutableMap;
import mockit.Delegate;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.Verifications;
import org.hamcrest.Matcher;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OpgJsonBaseTest {

    @Mocked
    protected PaymentGatewayManager paymentGatewayManager;
    @Mocked
    protected PaymentGatewayConfigurationHandler gatewayConfigurationHandler;
    @Mocked
    protected PaymentGatewayTransactionLogService transactionLogService;
    @Mocked
    protected PaymentMethodLogService paymentMethodLogService;
    @Mocked
    protected Connector httpsslConnector;
    @Mocked
    protected Connector httpsConnector;
    @Mocked
    protected Connector braintreeConnector;
    @Mocked
    protected AuthTokenProvider authTokenProvider;
    @Mocked
    protected AuthTokenTelemetrySignalService authTokenTelemetrySignalService;
    @Mocked
    protected PaymentGatewayTelemetrySignal paymentGatewayTelemetrySignal;
    @Mocked
    protected OPGPluginLoader opgPluginLoader;
    @Mocked
    protected TenantRegionService tenantRegionService;
    @Mocked
    private SpringBeanAutowiringSupport springBeanAutowiringSupport;
    @Mocked
    protected PropertiesService propertiesService;
    @Mocked
    private RequestResponseProcessorFactory requestResponseProcessorFactory;
    @Mocked
    private PaymentGatewayMetadataFieldMappingService paymentGatewayMetadataFieldMappingService;


    private static final String OPG_PLUGIN_FILE_PATH_PROP = "opg.plugin.base.path";
    private static final Map<String, OPGPluginService> opgPluginServiceMap = new HashMap<>();

    protected static void verifyConnectorRequest(
            Connector connector,
            ConnectorHeadersVerifier... headerVerifiers) {
        new Verifications() {{
            List<Map<String, String>> headers = new ArrayList<>();
            connector.send(withCapture(headers));

            if (headers.size() != headerVerifiers.length) {
                fail("The number of verifiers does not match the invocation times");
            }

            for (int i = 0; i < headerVerifiers.length; i++) {
                headerVerifiers[i].verify(headers.get(i));
            }
        }};
    }

    protected void expectingProcessor() {
        //we will return null, inorder to test the request-body in verifyConnectorRequest
        new Expectations() {{
            requestResponseProcessorFactory.getProcessor((String) anyString);
            result = null;
        }};
    }

    protected void expectingProcessor(RequestResponseProcessor requestResponseProcessor) {
        //we will return null, inorder to test the request-body in verifyConnectorRequest
        new Expectations() {{
            requestResponseProcessorFactory.getProcessor((String) anyString);
            result = requestResponseProcessor;
        }};
    }

    protected void expectingAuthToken(
            final AuthTokenProvider authTokenProvider,
            final AuthToken authToken) {
        new Expectations() {{
            authTokenProvider.getAuthToken((Map<String, String>) any);
            result = authToken;

            authTokenProvider.buildAuthHeader(authToken);
            result = ImmutableMap.of("Authorization", "Bearer " + authToken.getToken());

            authTokenProvider.isAuthTokenError((Map<String, String>) any, (String) any, (String) any);
            result = false;
        }};
    }

    protected void expectingConnectorResponse(
            final Connector connector,
            final Matcher<Map<String, String>> headerMatcher,
            final Map<String, Object> responseMap) {
        new Expectations() {{
            connector.send(with(new Delegate<Map<String, String>>() {
                boolean validate(Map<String, String> headers) {
                    return headerMatcher.matches(headers);
                }
            }));
            result = responseMap;
        }};
    }

    /*
     # This method can be used to simulate to send diff kind of responses for each invocation (exceptions, responseMaps etc.,)
    */
    protected void expectingConnectorResponses(
            final Connector connector,
            final Matcher<Map<String, String>> headerMatcher,
            Object ...responses) throws IllegalArgumentException{
        if(responses==null || responses.length==0 || Arrays.stream(responses).anyMatch(response -> !(response instanceof Exception || response instanceof Map)))
            throw new IllegalArgumentException("The responses parameter should contain only the objects of classes Exception or Map. Please check your arguments");

        new Expectations() {{
            connector.send(with(new Delegate<Map<String, String>>() {
                void validate(Map<String, String> headers) {
                    assertTrue(headerMatcher.matches(headers));
                }
            }));
            result = responses;
        }};
    }

    private void expectingOauthTokenRefreshIntervalSetting(
            final PaymentGatewaySettingRepository settingRepository,
            final String gatewayType,
            final String gatewayVersion,
            final long oauthTokenRefreshInterval) {
        new Expectations() {{
            settingRepository.getOauthTokenRefreshInterval(gatewayType, gatewayVersion);
            result = oauthTokenRefreshInterval;
        }};
    }

    private void expectingConnectorTimeoutSettings(
            final PaymentGatewaySettingRepository settingRepository,
            final String gatewayType,
            final String gatewayVersion,
            final int connectionTimeout,
            final int socketTimeout) {
        new Expectations() {{
            settingRepository.getConnectorConnectionTimeout(gatewayType, gatewayVersion);
            result = connectionTimeout;

            settingRepository.getConnectorSocketTimeout(gatewayType, gatewayVersion);
            result = socketTimeout;
        }};
    }

    protected void expectingGatewayInstanceSettings(
            final PaymentGatewayConfigurationHandler gatewayConfigurationHandler,
            final PaymentGateway paymentGateway,
            final Map<String, String> gatewaySettings) {
        new Expectations() {{
            gatewayConfigurationHandler.resolveGatewayConfigAttributes(paymentGateway, (List<PaymentGatewayAttribute>) any);
            result = gatewaySettings;
        }};
    }

    protected void expectingRegionServiceCachedAlphaTwoByThree(final String expectedInputThreeCode, final String expectedOutputTwoCode) {
        new Expectations() {{
            tenantRegionService.findCachedAlphaTwoByThree(expectedInputThreeCode);
            result = expectedOutputTwoCode;
        }};
    }

    protected void expectingRegionServiceCachedAlphaThreeByTwo(final String expectedInputTwoCode, final String expectedOutputThreeCode) {
        new Expectations() {{
            tenantRegionService.findCachedAlphaThreeByTwo(expectedInputTwoCode);
            result = expectedOutputThreeCode;
        }};
    }

    protected OpenPaymentGateway buildOpenPaymentGatewayForTest(boolean isLive, PaymentGatewaySettingRepository settingRepository) {
        return buildOpenPaymentGatewayForTest(isLive, settingRepository, null);
    }

    protected OpenPaymentGateway buildOpenPaymentGatewayForTest(boolean isLive, PaymentGatewaySettingRepository settingRepository, AuthTokenProviderType authTokenProviderType) {
        PaymentGatewaySettingRepositoryProxyImpl settingRepositoryProxy = new PaymentGatewaySettingRepositoryProxyImpl(settingRepository);
        if (authTokenProviderType == null) {
            authTokenProviderType = AuthTokenProviderType.CitiV1TokenProvider; // backward compatability for existing tests
        }

        // init tenant region service for ZUtility
        new Expectations() {{
            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(withAny(new ZUtility()));
            result = new Delegate<Void>() {
                void processInjectionBasedOnCurrentContext(Object target) {
                    if (target instanceof ZUtility) {
                        ReflectionTestUtils.setField(target, "tenantRegionService", tenantRegionService);
                    } else if (target instanceof OperationStateMachineImpl) {
                        ReflectionTestUtils.setField(target, "paymentGatewayTelemetrySignal", paymentGatewayTelemetrySignal);
                        ReflectionTestUtils.setField(target, "paymentGatewayMetadataFieldMappingService", paymentGatewayMetadataFieldMappingService);
                    } else {
                        throw new RuntimeException("Unknown target class " + target.getClass().getName());
                    }
                }
            };
            minTimes = 0;
        }};

        return OpenPaymentGatewayBuilder.newBuilder()
                .withPaymentGatewayManager(paymentGatewayManager)
                .withPaymentGatewayConfigurationHandler(gatewayConfigurationHandler)
                .withPaymentGatewayMetadataService(new PaymentGatewayMetadataServiceForTestImpl(JsonTestConfig.getInstance().getJsonFileResolver()))
                .withPaymentGatewayTransactionLogService(transactionLogService)
                .withPaymentMethodLogService(paymentMethodLogService)
                // setup unit test connector
                .withConnectorWhen(!isLive, ConnectorType.HTTPsSSL, httpsslConnector)
                .withConnectorWhen(!isLive, ConnectorType.HTTPs, httpsConnector)
                .withConnectorWhen(!isLive, ConnectorType.Braintree, braintreeConnector)
                // setup live test connector
                .withConnectorWhen(isLive, ConnectorType.HTTPsSSL, SimpleConnectorFactoryForLiveTest.factory(ConnectorType.HTTPsSSL))
                .withConnectorWhen(isLive, ConnectorType.HTTPs, SimpleConnectorFactoryForLiveTest.factory(ConnectorType.HTTPs))
                .withConnectorWhen(isLive, ConnectorType.Braintree, SimpleConnectorFactoryForLiveTest.factory(ConnectorType.Braintree))
                // setup test auth token provider
                .withAuthTokenProvider(authTokenProviderType, isLive ?
                        SimpleAuthTokenProviderFactoryForLiveTest.newFactory()
                                .withPaymentGatewaySettingRepository(settingRepositoryProxy)
                                .withAuthTokenTelemetrySignalService(authTokenTelemetrySignalService)
                                .factory(authTokenProviderType) : authTokenProvider)
                .withPaymentGatewaySettingRepository(settingRepositoryProxy)
                .withPaymentGatewayTelemetrySignal(paymentGatewayTelemetrySignal)
                .withOPGPluginLoader(opgPluginLoader)
                .withPropertiesService(propertiesService)
                .withPaymentGatewayMetadataFieldMappingService(paymentGatewayMetadataFieldMappingService)
                .build();
    }

    @Deprecated
    protected void expectingSystemConfigForTest(String key, String expectedValue) {
        new Expectations() {{
            new MockUp<ZUtility>() {
                @Mock
                public String getConfigValueFromKey(String keyString) {
                    return expectedValue;
                }
            };
        }};
    }

    public void loadPlugin(PaymentGatewayType paymentGatewayType) throws IOException {
        if (!opgPluginServiceMap.containsKey(paymentGatewayType.getName())) { // avoid reload of plugins for each test case
            String opgPluginPath = JsonTestConfig.getInstance().resolveProperty(OPG_PLUGIN_FILE_PATH_PROP);
            Path pluginJarPath = Paths.get(String.join(File.separator, opgPluginPath, paymentGatewayType.getName().toLowerCase(), "target", "plugin")); // build path
            List<Path> results = Files.find(pluginJarPath, 1,
                    (path, basicFileAttributes) -> path.getFileName().toString().equals(paymentGatewayType.getName().toLowerCase() + ".jar")).collect(Collectors.toList());
            if (results != null && !results.isEmpty()) {
                PluginManager pluginManager = new DefaultPluginManager(pluginJarPath);
                pluginManager.loadPlugins();
                pluginManager.startPlugins();
                List<OPGPluginService> extensions = pluginManager.getExtensions(paymentGatewayType.getName());
                opgPluginServiceMap.put(paymentGatewayType.getName(), extensions != null && !extensions.isEmpty() ? extensions.get(0) : null);
            } else {
                fail(String.format("Plugin jar does not exist at %s. Please run mvn clean install at %s",
                        pluginJarPath + File.separator + paymentGatewayType.getName().toLowerCase() + ".jar", OPG_PLUGIN_FILE_PATH_PROP));
            }
        }
        new Expectations() {{
            opgPluginLoader.getPluginService(paymentGatewayType.getName());
            result = opgPluginServiceMap.get(paymentGatewayType.getName());
        }};
    }
}
