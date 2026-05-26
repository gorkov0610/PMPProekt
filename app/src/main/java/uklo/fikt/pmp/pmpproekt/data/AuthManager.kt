package uklo.fikt.pmp.pmpproekt.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import uklo.fikt.pmp.pmpproekt.R

@Suppress("DEPRECATION")
class AuthManager(context: Context) {
    private val appContext = context.applicationContext
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val callbackManager = CallbackManager.Factory.create()
    private val db = FirebaseFirestore.getInstance()
    private var isDeletingAccountMode: Boolean = false

    fun getCallbackManager() = callbackManager
    fun getCurrentUser() = auth.currentUser

    fun setIsDeletingFlag(value: Boolean) {
        isDeletingAccountMode = value
    }
    fun isDeletingFlag(): Boolean {
        return isDeletingAccountMode
    }
    fun signOut(onComplete: (Boolean) -> Unit = {}) {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val userId = currentUser.uid
            db.collection("users").document(userId)
                .update("fcmToken", "")
                .addOnCompleteListener { task ->
                    auth.signOut()
                    LoginManager.getInstance().logOut()

                    if (task.isSuccessful) {
                        Log.d("AuthManager", "FCM Токенот е успешно избришан при одјава.")
                    } else {
                        Log.e("AuthManager", "Грешка при бришење на FCM токен", task.exception)
                    }

                    onComplete(task.isSuccessful)
                }
        } else {
            auth.signOut()
            LoginManager.getInstance().logOut()
            onComplete(true)
        }
    }

    fun saveUserToFirestore(customName: String? = null) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val finalName = customName
            ?: currentUser.displayName
            ?: if (currentUser.isAnonymous) appContext.getString(R.string.chat_loading) else appContext.getString(R.string.user)

        val finalProfilePicture = if (currentUser.photoUrl != null) {
            currentUser.photoUrl.toString()
        } else if (currentUser.isAnonymous) {
            "https://api.dicebear.com/7.x/bottts/png?seed=$userId"
        } else {
            "https://ui-avatars.com/api/?name=${finalName}&background=A7F3D0&color=065F46&size=128"
        }

        val user = User(
            uid = userId,
            name = finalName,
            email = currentUser.email ?: "",
            profilePicture = finalProfilePicture
        )

        db.collection("users").document(userId)
            .set(user, SetOptions.merge())
            .addOnSuccessListener { Log.d("Firestore", "Корисникот [$finalName] е успешно зачуван!") }
            .addOnFailureListener { e -> Log.e("Firestore", "Грешка при зачувување", e) }
    }

    fun handleFacebookLogin(activity : Activity, onResult: (Boolean, String?) -> Unit) {
        LoginManager.getInstance().logInWithReadPermissions(activity, listOf("email" ,"public_profile"))
        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    val credential = FacebookAuthProvider.getCredential(result.accessToken.token)
                    auth.signInWithCredential(credential)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                saveUserToFirestore()
                                onResult(true, null)
                            } else {
                                onResult(false, task.exception?.message)
                            }
                        }
                }
                override fun onCancel() { onResult(false, "Canceled") }
                override fun onError(error: FacebookException) { onResult(false, error.message) }
            }
        )
    }

    fun getGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(appContext.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(appContext, gso)
    }

    fun signInWithGoogle(idToken: String, onResult: (Boolean) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveUserToFirestore() // 🛠️ Автоматски зачувај во база
                }
                onResult(task.isSuccessful)
            }
    }

    fun signInAnonymously(onResult: (Boolean) -> Unit) {
        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 🛠️ Автоматски зачувај како Гостин со DiceBear роботче
                    saveUserToFirestore(customName = "Гостин")
                }
                onResult(task.isSuccessful)
            }
    }

    fun signUpWithEmail(email: String, pass: String, name: String, onResult: (Boolean, Throwable?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val profileUpdates = userProfileChangeRequest {
                        displayName = name
                    }
                    auth.currentUser?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener {
                            saveUserToFirestore(customName = name)
                            onResult(true, null)
                        }
                } else {
                    onResult(false, task.exception)
                }
            }
    }

    fun signInWithEmail(email: String, pass: String, onResult: (Boolean, Throwable?) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveUserToFirestore()
                }
                onResult(task.isSuccessful, task.exception)
            }
    }
    fun reauthenticateAndIdDeleteWithEmail(password: String, onComplete: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        val email = user?.email

        if (email != null && user != null) {
            val credential = EmailAuthProvider.getCredential(email, password)

            // 1. Повторно го најавуваме со лозинката што ја внесе во дијалогот
            user.reauthenticate(credential)
                .addOnSuccessListener {
                    // 2. Сесијата е сега свежа, веднаш го бришеме
                    user.delete()
                        .addOnSuccessListener { onComplete(true, null) }
                        .addOnFailureListener { e -> onComplete(false, e.localizedMessage) }
                }
                .addOnFailureListener { e -> onComplete(false, e.localizedMessage) }
        } else {
            onComplete(false, "Нема најавен корисник.")
        }
    }

    fun getErrorMessage(exception: Exception?): String {
        if (exception == null) return appContext.getString(R.string.error_unknown)
        return when (exception) {
            is FirebaseAuthWeakPasswordException -> appContext.getString(R.string.error_weak_password)
            is FirebaseAuthUserCollisionException -> appContext.getString(R.string.error_user_collision)
            is FirebaseAuthInvalidUserException -> appContext.getString(R.string.error_user_not_found)
            is FirebaseAuthInvalidCredentialsException -> appContext.getString(R.string.error_wrong_password)
            is FirebaseNetworkException -> appContext.getString(R.string.error_network)
            else -> appContext.getString(R.string.error_unknown)
        }
    }
}