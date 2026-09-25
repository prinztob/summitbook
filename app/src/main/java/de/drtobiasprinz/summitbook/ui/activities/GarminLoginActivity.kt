package de.drtobiasprinz.summitbook.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.ui.theme.SummitBookTheme
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Holds the login output state so it survives configuration changes while
 * the login [PythonActivity.Task] (also a ViewModel) keeps running.
 */
class GarminLoginViewModel : ViewModel() {
    val loginOutput = MutableStateFlow("")
    val loginInputNeeded = MutableStateFlow(false)
    val loginFinished = MutableStateFlow(false)
}

/**
 * Compose replacement for the raw Python console previously shown for the
 * Garmin Connect login. It reuses [PythonActivity.Task], which redirects
 * Python's stdout/stderr into [output] events and exposes stdin via
 * [inputEnabled]; the MFA prompt therefore appears as a proper dialog
 * instead of a console input field.
 */
class GarminLoginActivity : ComponentActivity() {

    private lateinit var task: PythonActivity.Task
    private lateinit var loginViewModel: GarminLoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        task = ViewModelProvider(this)[PythonActivity.Task::class.java]
        loginViewModel = ViewModelProvider(this)[GarminLoginViewModel::class.java]

        task.output.observe(this) { text ->
            if (text.contains("[Finished]")) {
                loginViewModel.loginFinished.value = true
            } else {
                loginViewModel.loginOutput.value += text
            }
        }
        task.inputEnabled.observe(this) { enabled ->
            loginViewModel.loginInputNeeded.value = java.lang.Boolean.TRUE == enabled
        }

        setContent {
            SummitBookTheme {
                GarminLoginScreen()
            }
        }
    }

    override fun onResume() {
        task.resumeStreams()
        super.onResume()
        if (task.state == Thread.State.NEW) {
            task.start()
        }
    }

    override fun onPause() {
        super.onPause()
        if (!isChangingConfigurations) {
            task.pauseStreams()
        }
    }

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    @Composable
    private fun GarminLoginScreen() {
        val output by loginViewModel.loginOutput.collectAsStateWithLifecycle()
        val inputNeeded by loginViewModel.loginInputNeeded.collectAsStateWithLifecycle()
        val finished by loginViewModel.loginFinished.collectAsStateWithLifecycle()
        var mfaCode by rememberSaveable { mutableStateOf("") }
        val scrollState = rememberScrollState()

        LaunchedEffect(output) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.garmin_login_title)) }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (finished) {
                    Text(
                        text = stringResource(R.string.garmin_login_done),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(onClick = { finish() }) {
                        Text(stringResource(R.string.close))
                    }
                } else {
                    Text(
                        text = stringResource(R.string.garmin_login_running),
                        style = MaterialTheme.typography.titleMedium
                    )
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                }

                if (output.isNotBlank()) {
                    Text(
                        text = output,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (inputNeeded && !finished) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text(stringResource(R.string.garmin_login_mfa_title)) },
                    text = {
                        OutlinedTextField(
                            value = mfaCode,
                            onValueChange = { mfaCode = it },
                            label = { Text(stringResource(R.string.garmin_login_mfa_hint)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                task.onInput("$mfaCode\n")
                                mfaCode = ""
                            },
                            enabled = mfaCode.isNotBlank()
                        ) {
                            Text(stringResource(R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { finish() }) {
                            Text(stringResource(R.string.close))
                        }
                    }
                )
            }
        }
    }
}
