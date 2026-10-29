package br.gov.sp.cps.previsaodotempo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.gov.sp.cps.previsaodotempo.ui.theme.PrevisaodotempoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrevisaodotempoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PrevisaoTempoApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun PrevisaoTempoApp(modifier: Modifier = Modifier) {
    var cidadeTexto by remember { mutableStateOf("") }
    var previsao by remember { mutableStateOf<Previsao?>(null) }
    var carregando by remember { mutableStateOf(false) }
    var mensagem by remember { mutableStateOf("Informe uma cidade e toque em buscar.") }

    val escopo = rememberCoroutineScope()
    val repository = remember { PrevisaoRepository() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Previsão do Tempo",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Text(
            text = "Consulte a previsão atual para uma cidade."
        )

        OutlinedTextField(
            value = cidadeTexto,
            onValueChange = { cidadeTexto = it },
            label = { Text("Nome da cidade") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val cidade = cidadeTexto.trim()
                if (cidade.isEmpty()) {
                    mensagem = "Por favor, informe o nome de uma cidade."
                    previsao = null
                    return@Button
                }

                escopo.launch {
                    carregando = true
                    mensagem = "Consultando a API..."
                    previsao = null

                    try {
                        val previsaoAtual = repository.buscarPrevisao(cidade)
                        previsao = previsaoAtual
                        mensagem = "Previsão consultada com sucesso."
                    } catch (e: Exception) {
                        mensagem = "Não foi possível consultar: " + (e.message ?: "erro inesperado")
                    } finally {
                        carregando = false
                    }
                }
            },
            enabled = !carregando,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Buscar Previsão")
        }

        if (carregando) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        Text(text = mensagem)

        previsao?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Cidade: ${it.nome}",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Temperatura atual: ${it.temperatura}°C",
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}
