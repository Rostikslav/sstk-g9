package com.sstk_g9.ecoswitch

/*import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons // Importieren für Standard-Icons
import androidx.compose.material.icons.filled.Favorite // Beispiel für ein Standard-Icon
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sstk_g9.ecoswitch.ui.theme.EcoSwitchTheme // Ersetze dies ggf. durch deinen Theme-Namen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // MyFirstComposeAppTheme ist dein App-Theme, das Farben, Typografie etc. definiert.
            // Es wird normalerweise automatisch erstellt, wenn du ein neues Compose-Projekt startest.
            EcoSwitchLokalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), // Füllt den gesamten verfügbaren Platz
                    color = MaterialTheme.colorScheme.background // Hintergrundfarbe aus dem Theme
                ) {
                    MyAppContent()
                }
            }
        }
    }
}

@Composable
fun MyAppContent() {
    // 'remember' speichert den Zustand über Recompositions hinweg.
    // 'mutableStateOf' erstellt einen beobachtbaren Zustand. Wenn sich dieser Wert ändert,
    // werden alle Composables, die diesen Zustand lesen, neu gezeichnet (recomposed).
    var greetingText by remember { mutableStateOf("Hallo Android!") }
    var timerStatus by remember { mutableStateOf("Timer status: ") }
    var buttonClicks by remember { mutableStateOf(0) }

    // Column ordnet Elemente vertikal untereinander an.
    /*Column(
        modifier = Modifier
            .fillMaxSize() // Füllt den gesamten Platz der Column
            .padding(16.dp), // Innenabstand für alle Seiten
        horizontalAlignment = Alignment.CenterHorizontally, // Zentriert Kind-Elemente horizontal
        verticalArrangement = Arrangement.Center // Zentriert Kind-Elemente vertikal
    ) {
        Text(
            text = timerStatus,
            fontSize = 24.sp, // Schriftgröße
            color = MaterialTheme.colorScheme.primary // Textfarbe aus dem Theme
        )

        Spacer(modifier = Modifier.height(24.dp)) // Vertikaler Abstand

        Button(onClick = {
            buttonClicks++
            greetingText = "Button $buttonClicks mal geklickt!"
        }) {
            Text("Klick Mich")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (buttonClicks > 0) {
            Text(
                text = "Du hast den Button schon $buttonClicks Mal gedrückt.",
                fontSize = 16.sp
            )
        }
    }*/
}

// Die @Preview-Annotation ermöglicht es dir, diese Composable-Funktion
// direkt in der Design-Ansicht von Android Studio zu sehen, ohne die App
// auf einem Emulator oder Gerät ausführen zu müssen.
@Preview(showBackground = true, name = "App Content Preview")
@Composable
fun DefaultPreview() {
    EcoSwitchLokalTheme {
        MyAppContent()
    }
}

 */