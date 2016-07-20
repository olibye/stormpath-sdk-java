package com.stormpath.sdk.servlet.mvc

import com.stormpath.sdk.account.Account
import com.stormpath.sdk.application.Application
import com.stormpath.sdk.authc.AuthenticationResult
import com.stormpath.sdk.directory.AccountStore
import com.stormpath.sdk.http.HttpMethod
import com.stormpath.sdk.impl.oauth.authc.DefaultAccessTokenResult
import com.stormpath.sdk.oauth.AccessTokenResult
import com.stormpath.sdk.servlet.application.ApplicationResolver
import com.stormpath.sdk.servlet.form.Form
import com.stormpath.sdk.servlet.http.MediaType
import com.stormpath.sdk.servlet.http.Saver
import com.stormpath.sdk.servlet.http.UserAgents
import com.stormpath.sdk.servlet.http.impl.DefaultUserAgent
import com.stormpath.sdk.servlet.mvc.provider.AccountStoreModel
import com.stormpath.sdk.servlet.mvc.provider.AccountStoreModelFactory
import com.stormpath.sdk.servlet.mvc.provider.DefaultAccountStoreModel
import com.stormpath.sdk.servlet.mvc.provider.DefaultSamlProviderModel
import com.stormpath.sdk.servlet.oauth.OAuthTokenResolver
import org.testng.annotations.Test

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static org.easymock.EasyMock.*
import static org.testng.Assert.*

/**
 * @since 1.0.0
 */
class LoginControllerTest {

    @Test
    public void testLoginPreHandlerAndContinueNormalWorkflow() {
        WebHandler loginPreHandler = createMock(WebHandler)
        Saver<AuthenticationResult> authenticationResultSaver = createMock(Saver)
        HttpServletRequest request = createMock(HttpServletRequest)
        HttpServletResponse response = createMock(HttpServletResponse)
        Form form = createMock(Form)
        AccessTokenResult accessTokenResult = new DefaultAccessTokenResult(null, null, null, null)

        expect(loginPreHandler.handle(request, response, null)).andReturn true
        expect(form.getFieldValue("login")).andReturn "login"
        expect(form.getFieldValue("password")).andReturn "password"

        expect(request.login("login", "password"))
        expect(request.getAttribute(OAuthTokenResolver.REQUEST_ATTR_NAME)).andReturn accessTokenResult
        expect(authenticationResultSaver.set(request, response, accessTokenResult))

        expect(request.getAttribute(UserAgents.USER_AGENT_REQUEST_ATTRIBUTE_NAME)).andReturn new DefaultUserAgent(request)
        expect(request.getHeader("Accept")).andReturn "text/html"
        expect(request.getParameter("next")).andReturn null

        //Minimum things required to test the onValidSubmit method only
        LoginController loginController = new LoginController(
                preLoginHandler: loginPreHandler,
                authenticationResultSaver: authenticationResultSaver,
                produces: Arrays.asList(MediaType.TEXT_HTML)
        )

        replay loginPreHandler, authenticationResultSaver, request, response, form

        def vm = loginController.onValidSubmit(request, response, form)

        verify loginPreHandler, authenticationResultSaver, request, response, form

        assertNotNull(vm, "ViewModel should not be empty")
    }

    @Test
    public void testLoginPreHandlerAndStopNormalWorkflow() {
        WebHandler loginPreHandler = createMock(WebHandler)
        HttpServletRequest request = createMock(HttpServletRequest)
        HttpServletResponse response = createMock(HttpServletResponse)
        Form form = createMock(Form)

        expect(loginPreHandler.handle(request, response, null)).andReturn false

        //Minimum things required to test the onValidSubmit method only
        LoginController loginController = new LoginController(
                preLoginHandler: loginPreHandler,
                produces: Arrays.asList(MediaType.TEXT_HTML)
        )

        replay loginPreHandler, request, response, form

        def vm = loginController.onValidSubmit(request, response, form)

        verify loginPreHandler, request, response, form

        assertNull(vm, "ViewModel should be empty")
    }

    @Test
    public void testLoginPostHandlerAndContinueNormalWorkflow() {
        WebHandler loginPostHandler = createMock(WebHandler)
        Saver<AuthenticationResult> authenticationResultSaver = createMock(Saver)
        HttpServletRequest request = createMock(HttpServletRequest)
        HttpServletResponse response = createMock(HttpServletResponse)
        Form form = createMock(Form)
        AccessTokenResult accessTokenResult = createMock(AccessTokenResult)
        Account account = createMock(Account)

        expect(form.getFieldValue("login")).andReturn "login"
        expect(form.getFieldValue("password")).andReturn "password"

        expect(request.login("login", "password"))
        expect(request.getAttribute(OAuthTokenResolver.REQUEST_ATTR_NAME)).andReturn((AccessTokenResult) accessTokenResult)
        expect(authenticationResultSaver.set(request, response, accessTokenResult))

        expect(request.getAttribute(UserAgents.USER_AGENT_REQUEST_ATTRIBUTE_NAME)).andReturn new DefaultUserAgent(request)
        expect(request.getHeader("Accept")).andReturn "text/html"
        expect(request.getParameter("next")).andReturn null

        expect(accessTokenResult.getAccount()).andReturn account
        expect(loginPostHandler.handle(request, response, account)).andReturn true

        //Minimum things required to test the onValidSubmit method only
        LoginController loginController = new LoginController(
                postLoginHandler: loginPostHandler,
                authenticationResultSaver: authenticationResultSaver,
                produces: Arrays.asList(MediaType.TEXT_HTML)
        )

        replay loginPostHandler, authenticationResultSaver, request, response, form, accessTokenResult

        def vm = loginController.onValidSubmit(request, response, form)

        verify loginPostHandler, authenticationResultSaver, request, response, form, accessTokenResult

        assertNotNull(vm, "ViewModel should not be empty")
    }

    @Test
    public void testLoginPostHandlerAndStopNormalWorkflow() {
        WebHandler loginPostHandler = createMock(WebHandler)
        Saver<AuthenticationResult> authenticationResultSaver = createMock(Saver)
        HttpServletRequest request = createMock(HttpServletRequest)
        HttpServletResponse response = createMock(HttpServletResponse)
        Form form = createMock(Form)
        AccessTokenResult accessTokenResult = createMock(AccessTokenResult)
        Account account = createMock(Account)

        expect(form.getFieldValue("login")).andReturn "login"
        expect(form.getFieldValue("password")).andReturn "password"

        expect(request.login("login", "password"))
        expect(request.getAttribute(OAuthTokenResolver.REQUEST_ATTR_NAME)).andReturn((AccessTokenResult) accessTokenResult)
        expect(authenticationResultSaver.set(request, response, accessTokenResult))

        expect(accessTokenResult.getAccount()).andReturn account
        expect(loginPostHandler.handle(request, response, account)).andReturn false

        //Minimum things required to test the onValidSubmit method only
        LoginController loginController = new LoginController(
                postLoginHandler: loginPostHandler,
                authenticationResultSaver: authenticationResultSaver,
                produces: Arrays.asList(MediaType.TEXT_HTML)
        )

        replay loginPostHandler, authenticationResultSaver, request, response, form, accessTokenResult

        def vm = loginController.onValidSubmit(request, response, form)

        verify loginPostHandler, authenticationResultSaver, request, response, form, accessTokenResult

        assertNull(vm, "ViewModel should be empty")
    }

    @Test
    public void verifyErrorWhenAccountStoresExistAndCallbackDisabled() {

        HttpServletRequest request = createMock(HttpServletRequest)
        HttpServletResponse response = createMock(HttpServletResponse)
        ApplicationResolver applicationResolver = createMock(ApplicationResolver)
        Application application = createMock(Application)
        AccountStore accountStore = createMock(AccountStore)
        expect(applicationResolver.getApplication((HttpServletRequest)same(request))).andReturn(application)

        AccountStoreModelFactory accountStoreModelFactory = createMock(AccountStoreModelFactory)
        Form form = createMock(Form)

        List<AccountStoreModel> accountStores = new ArrayList<AccountStoreModel>()
        accountStores.add(new DefaultAccountStoreModel(null, new DefaultSamlProviderModel(null)))
        expect(accountStoreModelFactory.getAccountStores(request)).andReturn(accountStores).times(2)
        expect(request.getAttribute(UserAgents.USER_AGENT_REQUEST_ATTRIBUTE_NAME)).andReturn new DefaultUserAgent(request)
        expect(request.getParameter("status")).andReturn null
        expect(request.getHeader("Accept")).andReturn "text/html"
        expect(request.getMethod()).andReturn HttpMethod.GET.name()
        expect(applicationResolver.getApplication((HttpServletRequest)same(request))).andStubReturn(application)
        expect(application.getDefaultAccountStore()).andStubReturn(accountStore)

        LoginController loginController = new LoginController(
                produces: Arrays.asList(MediaType.TEXT_HTML),
                accountStoreModelFactory: accountStoreModelFactory,
                samlAccountStoreModelFactory: accountStoreModelFactory,
                applicationResolver: applicationResolver,
                idSiteEnabled: false,
                callbackEnabled: false,
        )

        replay request, response, form, accountStoreModelFactory, applicationResolver, application, accountStore

        List<ErrorModel> errors = new ArrayList<>();
        Map<String, Object> model = new HashMap<>();

        loginController.appendModel(request, response, form, errors, model)
        assertEquals errors.size(), 1, "Errors not added when configuration mismatch"

        verify request, response, form, accountStoreModelFactory, applicationResolver, application, accountStore
    }

    /**
     * @see <a href="https://github.com/stormpath/stormpath-sdk-java/issues/333">Issue 333</a>
     */
    @Test
    public void testIsRegisterEnabledWhenPropertyIsFalse() {

        HttpServletRequest request = createMock(HttpServletRequest)

        LoginController controller = new LoginController(registerEnabled: false)

        replay request

        assertFalse controller.isRegisterEnabled(request)

        verify request
    }

    /**
     * @see <a href="https://github.com/stormpath/stormpath-sdk-java/issues/333">Issue 333</a>
     */
    @Test
    public void testIsRegisterEnabledWhenPropertyIsTrueAndNoAppDefaultAccountStore() {

        HttpServletRequest request = createMock(HttpServletRequest)
        ApplicationResolver applicationResolver = createMock(ApplicationResolver)
        Application application = createMock(Application)

        LoginController controller = new LoginController(applicationResolver: applicationResolver)

        expect(applicationResolver.getApplication((HttpServletRequest)same(request))).andReturn(application)
        expect(application.getDefaultAccountStore()).andReturn(null)

        replay request, applicationResolver, application

        assertFalse controller.isRegisterEnabled(request)

        verify request, applicationResolver, application
    }
}