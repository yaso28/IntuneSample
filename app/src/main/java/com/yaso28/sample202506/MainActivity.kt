package com.yaso28.sample202506

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication.ISingleAccountApplicationCreatedListener
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication.CurrentAccountCallback
import com.microsoft.identity.client.ISingleAccountPublicClientApplication.SignOutCallback
import com.microsoft.identity.client.PublicClientApplication.createSingleAccountPublicClientApplication
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.intune.mam.client.app.MAMComponents
import com.microsoft.intune.mam.client.identity.MAMPolicyManager
import com.microsoft.intune.mam.policy.MAMEnrollmentManager
import com.microsoft.intune.mam.policy.MAMServiceAuthenticationCallback

class MainActivity : AppCompatActivity() {
    private lateinit var msalApp: ISingleAccountPublicClientApplication
    private lateinit var mamEnrollmentManager: MAMEnrollmentManager

    private lateinit var txtUser: TextView
    private lateinit var txtLog: TextView

    private var currentAccount: IAccount? = null

    private lateinit var btnSignIn: Button
    private lateinit var btnSignOut: Button
    private lateinit var btnShowStatus: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        txtUser = findViewById(R.id.txtUser)
        txtLog = findViewById(R.id.txtLog)
        btnSignIn = findViewById(R.id.btnSignIn)
        btnSignIn.setOnClickListener{
            signIn()
        }
        btnSignOut = findViewById(R.id.btnSignOut)
        btnSignOut.setOnClickListener {
            signOut()
        }
        btnShowStatus = findViewById(R.id.btnShowStatus)
        btnShowStatus.setOnClickListener {
            showStatus()
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initMam()
        initMsal()
    }

    private fun initMsal() {
        createSingleAccountPublicClientApplication(
            applicationContext,
            R.raw.msal_config,
            object: ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    logInfo("initMsal", "created")
                    msalApp = application

                    sso()
                }

                override fun onError(exception: MsalException) {
                    logError("initMsal", "error: ${exception.message}")
                }

            }
        )
    }

    private fun sso() {
        msalApp.getCurrentAccountAsync(object: CurrentAccountCallback {
            override fun onAccountLoaded(activeAccount: IAccount?) {
                if (activeAccount == null) {
                    logInfo("sso", "onAccountLoaded: account is null")
                    signIn()
                } else {
                    logInfo("sso", "onAccountLoaded: account exists")
                    setAccount(activeAccount)
                }
            }

            override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                if (currentAccount == null) {
                    logInfo("sso", "onAccountChanged: account is null")
                    signIn()
                } else {
                    logInfo("sso", "onAccountChanged: account exists")
                    setAccount(currentAccount)
                }
            }

            override fun onError(exception: MsalException) {
                logError("sso", "error: ${exception.message}")
            }
        })
    }

    private fun signIn() {
        val params = SignInParameters.builder()
            .withActivity(this)
            .withScopes(listOf("User.Read"))
            .withCallback(object: AuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    logInfo("signIn", "success")
                    setAccount(authenticationResult.account)
                }

                override fun onError(exception: MsalException) {
                    logError("signIn", "error: ${exception.message}")
                }

                override fun onCancel() {
                    logError("signIn", "cancel")
                }
            })
            .build()

        msalApp.signIn(params)
    }

    private fun signOut() {
        msalApp.signOut(object: SignOutCallback {
            override fun onSignOut() {
                logInfo("signOut", "success")
                clearAccount()
            }

            override fun onError(exception: MsalException) {
                logError("signOut", "error: ${exception.message}")
            }

        })
    }

    private fun initMam() {
        mamEnrollmentManager = MAMComponents.get(MAMEnrollmentManager::class.java)!!
        mamEnrollmentManager.registerAuthenticationCallback(object: MAMServiceAuthenticationCallback {
            override fun acquireToken(upn: String, aadId: String, resourceId: String): String? {
                val accessToken = acquireTokenSilent(listOf("${resourceId}/.default"))

                logInfo("MAM.acquireToken", "return: ${accessToken?.take(10) ?: "null"}")
                return  accessToken
            }
        })
    }

    @WorkerThread
    private fun acquireTokenSilent(scopes: List<String>): String? {
        try {
            logInfo("acquireTokenSilent", "scopes: ${scopes[0]}")

            val account = msalApp.currentAccount?.currentAccount
            if (account == null) {
                logError("MSAL", "acquireTokenSilent: account is null")
                return null
            }

            val params = AcquireTokenSilentParameters.Builder()
                .forAccount(account)
                .fromAuthority(account.authority)
                .withScopes(scopes)
                .build()
            val result = msalApp.acquireTokenSilent(params)
            val accessToken = result?.accessToken
            logInfo("acquireTokenSilent", "return: ${accessToken?.take(10) ?: "null"}")
            return accessToken
        } catch (e: Exception) {
            logError("acquireTokenSilent", "error: ${e.message}")
            return null
        }
    }

    private fun setAccount(account: IAccount) {
        logInfo("setAccount", account.username)
        logInfo("setAccount", account.id)
        logInfo("setAccount", account.tenantId)
        logInfo("setAccount", account.authority)
        mamEnrollmentManager.registerAccountForMAM(
            account.username,
            account.id,
            account.tenantId,
            account.authority
        )

        txtUser.text = account.username
        currentAccount = account
    }

    private fun clearAccount() {
        logInfo("clearAccount", "${currentAccount?.username}")

        val account = currentAccount
        if (account != null) {
            mamEnrollmentManager.unregisterAccountForMAM(
                account.username,
                account.id
            )
        }

        txtUser.text = "(GUEST)"
        currentAccount = null
    }

    private fun showStatus() {
        MAMPolicyManager.showDiagnostics(applicationContext)

        val account = currentAccount
        if (account == null) {
            logError("showStatus", "No Account")
        } else {
            val result = mamEnrollmentManager.getRegisteredAccountStatus(
                account.username,
                account.id
            )
            if (result == null) {
                logError("showStatus", "No Result")
            } else {
                logInfo("showStatus", result.name)
            }
        }
    }

    private fun logInfo(tag: String, msg:String) {
        Log.i(tag, msg)
        logText("INFO", tag, msg)
    }

    private fun logError(tag: String, msg: String) {
        Log.e(tag, msg)
        logText("ERROR", tag, msg)
    }

    private fun logText(level: String, tag: String, msg: String) {
        runOnUiThread {
            txtLog.append("\n[${level}][${tag}]${msg}")
        }
    }
}