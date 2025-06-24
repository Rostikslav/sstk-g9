package com.sstk_g9.ecoswitch.logic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object TimerManager {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Angezeigte Zeit
    var displayHours by mutableIntStateOf(0)
        private set
    var displayMinutes by mutableIntStateOf(0)
        private set
    var displaySeconds by mutableIntStateOf(0)
        private set

    // Eingabefelder - jetzt im TimerManager
    var editHoursInput by mutableStateOf("00")
    var editMinutesInput by mutableStateOf("00")
    var editSecondsInput by mutableStateOf("00")

    var isTimerRunning by mutableStateOf(false)
        private set
    private var remainingTimeInMillis by mutableLongStateOf(0L) // UI braucht diesen Wert nicht direkt zu kennen

    var isEditingTime by mutableStateOf(false)
        private set

    private var timerJob: Job? = null
    private var onTimerFinishedCallback: (() -> Unit)? = null

    private fun timeToMillis(h: Int, m: Int, s: Int): Long {
        return (h * 3600L + m * 60L + s) * 1000L
    }

    private fun millisToHMS(millis: Long, updateDisplay: Boolean = true) {
        val totalSeconds = millis / 1000
        val h = (totalSeconds / 3600).toInt()
        val m = ((totalSeconds % 3600) / 60).toInt()
        val s = (totalSeconds % 60).toInt()
        if (updateDisplay) {
            displayHours = h
            displayMinutes = m
            displaySeconds = s
        }
    }
    fun startOrStopTimer() {
        if (isTimerRunning) {
            stopTimer() // Ruft die interne stopTimer() auf
        } else {
            if (isEditingTime) {
                saveEditedTimeAndExitEditMode()
            }
            startTimer() // <<<< HIER WIRD startTimer() AUFGERUFEN!
        }
    }

    private fun startTimer() {
        if (isEditingTime) return // Sollte durch saveEditedTimeAndExitEditMode bereits false sein

        // Verwende die display Werte, falls remainingTimeInMillis 0 ist (z.B. nach initialer Eingabe)
        if (remainingTimeInMillis == 0L && (displayHours > 0 || displayMinutes > 0 || displaySeconds > 0)) {
            remainingTimeInMillis = timeToMillis(displayHours, displayMinutes, displaySeconds)
        } else if (remainingTimeInMillis == 0L) { // Keine Zeit gesetzt, nichts tun
            return
        }


        if (remainingTimeInMillis > 0 && !isTimerRunning) {
            isTimerRunning = true
            timerJob?.cancel()
            timerJob = scope.launch {
                var currentTime = remainingTimeInMillis
                while (currentTime > 0 && isTimerRunning) {
                    millisToHMS(currentTime)
                    delay(1000)
                    currentTime -= 1000
                    if (isTimerRunning) {
                        remainingTimeInMillis = currentTime
                    }
                }
                if (currentTime <= 0 && isTimerRunning) {
                    handleTimerFinished()
                }
            }
        }
    }

    private fun stopTimer() {
        if (isTimerRunning) {
            isTimerRunning = false
            timerJob?.cancel()
        }
    }

    private fun handleTimerFinished() {
        millisToHMS(0)
        isTimerRunning = false
        remainingTimeInMillis = 0L
        onTimerFinishedCallback?.invoke()
    }

    /**
     * Startet den Bearbeitungsmodus.
     * Initialisiert die Eingabefelder mit der aktuellen Anzeigezeit.
     */
    fun enterEditMode() {
        if (isTimerRunning) return // Bearbeitung nicht erlauben, wenn Timer läuft

        editHoursInput = "%02d".format(displayHours)
        editMinutesInput = "%02d".format(displayMinutes)
        editSecondsInput = "%02d".format(displaySeconds)
        isEditingTime = true
    }

    /**
     * Speichert die aktuell in den Input-Feldern stehende Zeit,
     * aktualisiert die Anzeige und beendet den Bearbeitungsmodus.
     */
    fun saveEditedTimeAndExitEditMode() {
        if (!isEditingTime) return

        val h = editHoursInput.toIntOrNull() ?: displayHours
        val m = editMinutesInput.toIntOrNull() ?: displayMinutes
        val s = editSecondsInput.toIntOrNull() ?: displaySeconds

        // Stoppe den Timer, falls er doch irgendwie lief (Sicherheitsmaßnahme)
        if (isTimerRunning) stopTimer()

        displayHours = h.coerceIn(0, 99)
        displayMinutes = m.coerceIn(0, 59)
        displaySeconds = s.coerceIn(0, 59)
        remainingTimeInMillis = timeToMillis(displayHours, displayMinutes, displaySeconds)
        isEditingTime = false
    }

    /**
     * Bricht den Bearbeitungsmodus ab, ohne Änderungen zu speichern.
     * Setzt die Eingabefelder nicht zurück, da sie beim nächsten `enterEditMode` neu befüllt werden.
     */
    fun cancelEditMode() {
        isEditingTime = false
    }


    /**
     * Überprüft, ob der Timer gestartet werden kann (Zeit gesetzt oder läuft bereits zum Stoppen).
     */
    fun canStartOrStop(): Boolean {
        return (remainingTimeInMillis > 0 || (displayHours > 0 || displayMinutes > 0 || displaySeconds > 0)) || isTimerRunning
    }

    fun setInitialTime(hours: Int, minutes: Int, seconds: Int) {
        if (!isTimerRunning && !isEditingTime) {
            displayHours = hours.coerceIn(0, 99)
            displayMinutes = minutes.coerceIn(0, 59)
            displaySeconds = seconds.coerceIn(0, 59)
            remainingTimeInMillis = timeToMillis(displayHours, displayMinutes, displaySeconds)
            millisToHMS(remainingTimeInMillis) // Stellt sicher, dass die Anzeige konsistent ist
        }
    }
}