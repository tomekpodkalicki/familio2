package pl.podkal.domowniczeqqq.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pl.podkal.domowniczeq.R

private val FamilioTeal = Color(0xFF1ABC9C)
private val FamilioDark = Color(0xFF222222)
private val FamilioGray = Color(0xFF777777)

@Composable
fun LoginScreenPreviewOnly() {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Logo Familio",
            modifier = Modifier
                .width(250.dp)
                .height(250.dp)
                .padding(top = 30.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Zaloguj się",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = FamilioDark
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Wypełnij formularz, aby kontynuować",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = FamilioGray
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Adres e-mail") },
            leadingIcon = { Icon(Icons.Rounded.Email, null) },
            shape = RoundedCornerShape(15.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Hasło") },
            leadingIcon = { Icon(Icons.Rounded.Lock, null) },
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(15.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Column(modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {},
                modifier = Modifier
                    .width(200.dp)
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FamilioTeal, contentColor = Color.White)
            ) {
                Text("Zaloguj się", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {},
                modifier = Modifier
                    .width(200.dp)
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FamilioDark, contentColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Zaloguj przez ", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(7.dp))
                    Image(
                        painter = painterResource(id = R.drawable.google_icon),
                        contentDescription = "Google Icon",
                        modifier = Modifier.size(24.dp)

                    )
                }
            }

            Text(
                "Zapomniałeś hasła?",
                color = FamilioGray,
                modifier = Modifier.clickable { },
                fontWeight = FontWeight.Bold
            )

            Text(
                "Zarejestruj konto",
                color = FamilioGray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { }
            )
        }
    }
}

@Preview
@Composable
fun LoginScreenPreview() {
    LoginScreenPreviewOnly()
}