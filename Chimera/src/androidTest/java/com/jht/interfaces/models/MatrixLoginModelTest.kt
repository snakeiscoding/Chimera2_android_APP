package com.jht.interfaces.models

import com.jht.chimera.lib.common.dapi.DapiLogin
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.data.LoginData
import com.jht.data.LoginType
import com.jht.data.QrAuthorizationResponse
import com.jht.data.QrLoginHandler
import com.jht.data.UserFieldDigits
import com.jht.data.UserFieldFunction
import com.jht.data.UserFieldList
import com.jht.data.UserFieldText
import com.jht.datamatrixlib.DataMatrix
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltAndroidTest
class MatrixLoginModelTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var loginModel: LoginModel

    @Before
    fun setup() {
        println("setup")
        hiltRule.inject()
    }

    @After
    fun cleanup() {
        println("cleanup")
        loginModel.close()
    }

    private fun getLoginData(loginType: LoginType): LoginData? {
        val loginList = loginModel.getLoginOptions()
        return loginList.find { it.type == loginType }
    }

    @Test
    fun testXidData() {
        var loginData: LoginData? = null

        DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_XID_LOGIN_BOOL, false)
        loginData = getLoginData(LoginType.Xid)
        Assert.assertTrue("Login is absent if disabled", loginData == null)

        DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_XID_LOGIN_BOOL, true)
        loginData = getLoginData(LoginType.Xid)
        Assert.assertTrue("Login is present if enabled", loginData != null)

        assertHasIdAndPassword(loginData)

        val fieldId = loginData?.find(UserFieldFunction.LoginId) as? UserFieldDigits
        val fieldPw = loginData?.find(UserFieldFunction.Password) as? UserFieldDigits

        Assert.assertTrue("Login ID is digits", fieldId != null)
        Assert.assertTrue("Login ID min length, 4 for legacy", fieldId?.minLength == 4) // 4 is allowed for legacy logins, but 10 is needed for new ones
        Assert.assertTrue("Login ID max length", fieldId?.maxLength == 14)

        Assert.assertTrue("Login Password is digits", fieldPw != null)
        Assert.assertTrue("Login Password min length", fieldPw?.minLength == 1)
        Assert.assertTrue("Login Password max length", fieldPw?.maxLength == 4)
    }

    @Test
    fun testGuestData() {
        var loginData: LoginData? = null

        DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ALLOW_GUEST_BOOL, false)
        loginData = getLoginData(LoginType.Guest)
        Assert.assertTrue("Login is absent if disabled", loginData == null)

        DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ALLOW_GUEST_BOOL, true)
        loginData = getLoginData(LoginType.Guest)
        Assert.assertTrue("Login is present if enabled", loginData != null)

        Assert.assertTrue("Guest login has no fields", loginData?.fields!!.isEmpty())
    }

    @Test
    fun testQrSessionStopUnregistersHandler() {
        val codeCount = AtomicInteger()
        val handler = countingHandler(codeCount)

        loginModel.qrStartSession(handler)
        DataMatrix.putParcelable(DapiLogin.Keys.EVENT_QR_CODE_PARCELABLE, sampleResponse("first"))
        Assert.assertEquals("handler receives code before stop", 1, codeCount.get())

        loginModel.qrStopSession()
        DataMatrix.putParcelable(DapiLogin.Keys.EVENT_QR_CODE_PARCELABLE, sampleResponse("post-stop"))
        Assert.assertEquals("handler is not invoked after stop", 1, codeCount.get())
    }

    @Test
    fun testQrSessionRestartReplacesHandler() {
        val firstCount = AtomicInteger()
        val secondCount = AtomicInteger()

        loginModel.qrStartSession(countingHandler(firstCount))
        loginModel.qrStartSession(countingHandler(secondCount))

        DataMatrix.putParcelable(DapiLogin.Keys.EVENT_QR_CODE_PARCELABLE, sampleResponse("restart"))

        Assert.assertEquals("prior handler is dropped on restart", 0, firstCount.get())
        Assert.assertEquals("new handler receives the event", 1, secondCount.get())

        loginModel.qrStopSession()
    }

    @Test
    fun testXidRegisterData() {
        DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_XID_LOGIN_BOOL, false)
        var registrations = loginModel.getRegisterOptions()
        var registerData = registrations.find { it.type == LoginType.Xid }
        Assert.assertTrue("Register is absent if disabled", registerData == null)

        DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_XID_LOGIN_BOOL, true)
        registrations = loginModel.getRegisterOptions()
        registerData = registrations.find { it.type == LoginType.Xid }
        Assert.assertTrue("Register is present if enabled", registerData != null)

        val fieldFunctions = listOf(
            UserFieldFunction.LoginId,
            UserFieldFunction.Password,
            UserFieldFunction.EmailAddress,
            UserFieldFunction.FirstName,
            UserFieldFunction.LastName,
            UserFieldFunction.Birthdate,
            UserFieldFunction.Gender,
            UserFieldFunction.UnitPreference,
            UserFieldFunction.Weight,
        )
        assertHasField(registerData, fieldFunctions)

        // new logins must be at least 10 (4 is allowed for existing legacy logins only)
        val field = registerData?.findCast<UserFieldText>(UserFieldFunction.LoginId)
        Assert.assertTrue("XID register is minimum 10 for new logins", field?.minLength == 10)
    }

}

private fun countingHandler(codeCount: AtomicInteger): QrLoginHandler = object : QrLoginHandler {
    override fun onCodeReady(response: QrAuthorizationResponse) { codeCount.incrementAndGet() }
    override fun onExpired() {}
    override fun onError() {}
    override fun onLoginSuccess() {}
}

private fun sampleResponse(userCode: String): QrAuthorizationResponse =
    QrAuthorizationResponse(
        deviceCode = "dc-$userCode",
        userCode = userCode,
        verificationUri = "https://example.test/verify",
        verificationUriComplete = "https://example.test/verify?user_code=$userCode",
        expiresIn = 180,
        interval = 5,
    )

private fun assertHasIdAndPassword(loginData: LoginData?) {
    val fieldFunctions = listOf(UserFieldFunction.LoginId, UserFieldFunction.Password)
    assertHasField(loginData, fieldFunctions)
}

private fun assertHasField(fieldList: UserFieldList?, functions: Iterable<UserFieldFunction>) {
    functions.forEach {
        Assert.assertTrue("Requires field ${it.name}", fieldList?.find(it) != null)
    }
}
