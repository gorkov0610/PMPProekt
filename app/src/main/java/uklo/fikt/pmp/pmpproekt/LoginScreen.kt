package uklo.fikt.pmp.pmpproekt

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import uklo.fikt.pmp.pmpproekt.data.AuthManager
import uklo.fikt.pmp.pmpproekt.ui.theme.EmeraldPrimary
import uklo.fikt.pmp.pmpproekt.ui.theme.SlateSecondary
import androidx.compose.ui.res.stringResource

import com.facebook.login.LoginManager
@Composable
fun LoginScreen(onLoginSuccess : () -> Unit){
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    DisposableEffect(Unit) {
        authManager.handleFacebookLogin { success, errorMessage ->
            if (success) {
                onLoginSuccess()
            } else {
                Log.e("FB_Auth", "Грешка: $errorMessage")
            }
        }
        onDispose {}
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { token ->
                authManager.signInWithGoogle(token) { success ->
                    if (success) {
                        onLoginSuccess()
                    } else {
                        Log.e("AuthError", "Firebase не го прифати токенот")
                    }
                }
            }
        } catch (e: ApiException) {
            Log.e("Auth", "Грешка од Google: ${e.statusCode}")
        }
    }


    Scaffold (
        containerColor = Color(0xFFF8F9FA)
    ){ paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ){
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldPrimary
                )

                Text(
                    text = stringResource(R.string.slogan),
                    fontSize = 16.sp,
                    color = SlateSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(64.dp))
                Button(
                    onClick = {
                        val signInIntent = authManager.getGoogleSignInClient().signInIntent
                        launcher.launch(signInIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    border = BorderStroke(1.dp, Color.LightGray),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Image(
                            painter = painterResource(R.drawable.google_logo),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.signin_google),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        LoginManager.getInstance().logInWithReadPermissions(
                            context as Activity,
                            listOf("email", "public_profile")
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ){
                        Icon(
                            painter = painterResource(id = R.drawable.facebook_logo),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.signin_fb), color = Color.White)
                    }
                }
            }
        }
    }
}