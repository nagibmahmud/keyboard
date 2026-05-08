package com.example.transparentkeyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class TransparentKeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private var kv: KeyboardView? = null
    private var keyboard: Keyboard? = null
    private var caps = false
    private var isSymbols = false
    private var isBangla = false
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var autoCaps = true
    private var lastSpaceWasCaps = false
    private var clipboardManager: ClipboardManager? = null
    private var isShiftLocked = false

    // AI Input Handling
    private var aiInputFieldHasFocus = false
    private var aiInputBuffer = StringBuilder()

    // UI Elements
    private var aiSuggestionBar: LinearLayout? = null
    private var aiPanel: LinearLayout? = null
    private var aiPanelTitle: TextView? = null
    private var aiResultText: TextView? = null
    private var aiInputField: EditText? = null
    private var aiModeTabs: LinearLayout? = null
    private var aiChips = arrayOfNulls<Button>(5)

    // AI State
    private var currentAIMode = "assistant"
    private var typingHistory = mutableListOf<String>()
    private var currentWord = ""

    // AI Knowledge Bases
    private val aiSuggestions = mapOf(
        "hello" to listOf("Hello!", "Hi there!", "Hey! How are you?", "Good morning!"),
        "thank" to listOf("Thank you!", "Thanks!", "Thank you so much!", "Thanks a lot!"),
        "how" to listOf("How are you?", "How's it going?", "How can I help?", "How about that?"),
        "good" to listOf("Good morning!", "Good afternoon!", "Good evening!", "Good night!"),
        "see" to listOf("See you later!", "See you soon!", "See you!"),
        "yes" to listOf("Yes, absolutely!", "Sure thing!", "Definitely!", "Of course!"),
        "no" to listOf("No, thank you", "Not really", "I don't think so", "No problem"),
        "meeting" to listOf("Sounds good!", "I'll be there", "What time?", "Let me check my schedule"),
        "love" to listOf("I love you!", "Love it!", "So lovely!", "Lots of love"),
        "happy" to listOf("That's great! 😊", "Awesome! 🎉", "Wonderful! ✨", "Amazing! 💪")
    )

    private val translationDB = mapOf(
        "hello" to mapOf("bn" to "হ্যালো", "es" to "Hola", "fr" to "Bonjour", "de" to "Hallo", "jp" to "こんにちは"),
        "thank you" to mapOf("bn" to "ধন্যবাদ", "es" to "Gracias", "fr" to "Merci", "de" to "Danke", "jp" to "ありがとう"),
        "how are you" to mapOf("bn" to "আপনি কেমন আছেন", "es" to "¿Cómo estás?", "fr" to "Comment allez-vous?", "de" to "Wie geht es Ihnen?", "jp" to "お元気ですか？"),
        "good morning" to mapOf("bn" to "সুপ্রভাত", "es" to "Buenos días", "fr" to "Bonjour", "de" to "Guten Morgen", "jp" to "おはようございます"),
        "good night" to mapOf("bn" to "শুভ রাত্রি", "es" to "Buenas noches", "fr" to "Bonne nuit", "de" to "Gute Nacht", "jp" to "おやすみなさい"),
        "i love you" to mapOf("bn" to "আমি তোমাকে ভালোবাসি", "es" to "Te amo", "fr" to "Je t'aime", "de" to "Ich liebe dich", "jp" to "愛してます"),
        "yes" to mapOf("bn" to "হ্যাঁ", "es" to "Sí", "fr" to "Oui", "de" to "Ja", "jp" to "はい"),
        "no" to mapOf("bn" to "না", "es" to "No", "fr" to "Non", "de" to "Nein", "jp" to "いいえ"),
        "please" to mapOf("bn" to "অনুগ্রহ করে", "es" to "Por favor", "fr" to "S'il vous plaît", "de" to "Bitte", "jp" to "お願いします"),
        "sorry" to mapOf("bn" to "দুঃখিত", "es" to "Lo siento", "fr" to "Désolé", "de" to "Entschuldigung", "jp" to "すみません"),
        "bye" to mapOf("bn" to "বিদায়", "es" to "Adiós", "fr" to "Au revoir", "de" to "Auf Wiedersehen", "jp" to "さようなら")
    )

    private val grammarRules = mapOf(
        "i " to "I ",
        " im " to " I'm ",
        " dont " to " don't ",
        " cant " to " can't ",
        " wont " to " won't ",
        " didnt " to " didn't ",
        " wouldnt " to " wouldn't ",
        " couldnt " to " couldn't ",
        " shouldnt " to " shouldn't ",
        " isnt " to " isn't ",
        " arent " to " aren't ",
        " wasnt " to " wasn't ",
        " doesnt " to " doesn't ",
        "ive " to "I've ",
        "youre " to "you're ",
        "theyre " to "they're ",
        "weve " to "we've ",
        "hes " to "he's ",
        "shes " to "she's ",
        "thier " to "their ",
        "recieve " to "receive ",
        "occured " to "occurred ",
        "seperate " to "separate ",
        "definately " to "definitely "
    )

    private val tonePresets = mapOf(
        "formal" to mapOf("hi" to "Hello", "thanks" to "Thank you", "bye" to "Goodbye", "ok" to "Understood", "yeah" to "Yes", "nah" to "No"),
        "casual" to mapOf("Hello" to "Hey", "Thank you" to "Thanks!", "Goodbye" to "See ya", "Understood" to "Got it", "Yes" to "Yeah", "No" to "Nah"),
        "friendly" to mapOf("Hello" to "Hi there! 😊", "Thank you" to "Thanks so much! 🙏", "Good" to "Awesome! ✨", "Okay" to "Perfect! 👍"),
        "professional" to mapOf("I think" to "I believe", "maybe" to "potentially", "but" to "however", "so" to "therefore", "also" to "additionally")
    )

    private val emojiDB = mapOf(
        "happy" to "😊😄🎉👍✨💪🌟😁",
        "sad" to "😢😞😔💔😥",
        "love" to "❤️😍💕💖💗💘💝🥰",
        "angry" to "😡😠🤬💢👿",
        "thinking" to "🤔💭🧐💡👀",
        "celebrate" to "🎉🎊🥳🎈🎁🏆⭐",
        "work" to "💼👨‍💻👩‍💻💻📊📈",
        "food" to "🍕🍔🍟🌮🍜🍣🍰☕",
        "weather" to "☀️🌤️⛅🌧️❄️🌈🌪️",
        "time" to "⏰⌚🕐📅🗓️⏳"
    )

    private val commonWords = listOf(
        "the", "there", "they", "their", "them", "then", "than", "that", "this", "these", "those",
        "hello", "help", "here", "have", "had", "has", "how", "what", "when", "where", "which",
        "who", "whom", "whose", "why", "will", "would", "could", "should", "may", "might",
        "thank", "thanks", "thinking", "thought", "through", "think", "thing", "things",
        "great", "good", "going", "got", "give", "get", "just", "make", "made",
        "really", "right", "very", "much", "more", "most", "some", "same",
        "please", "pleased", "pleasure",
        "morning", "night", "afternoon", "evening",
        "today", "tomorrow", "yesterday",
        "beautiful", "awesome", "amazing", "excellent", "perfect",
        "important", "interesting", "different", "special",
        "because", "before", "after", "between", "under", "over",
        "about", "above", "below", "against", "during", "without",
        "information", "question", "answer", "problem", "solution",
        "work", "working", "worked", "worker",
        "time", "times", "timing",
        "person", "people", "personnel",
        "world", "worlds",
        "hand", "hands", "handle",
        "part", "parts", "party",
        "place", "places",
        "case", "cases",
        "week", "weeks",
        "company", "system", "program",
        "development", "management", "department"
    )

    override fun onCreate() {
        super.onCreate()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    override fun onCreateInputView(): View {
        val rootView = layoutInflater.inflate(R.layout.input_view, null)

        kv = rootView.findViewById(R.id.keyboard)
        aiSuggestionBar = rootView.findViewById(R.id.aiSuggestionBar)
        aiPanel = rootView.findViewById(R.id.aiPanel)
        aiPanelTitle = rootView.findViewById(R.id.aiPanelTitle)
        aiResultText = rootView.findViewById(R.id.aiResultText)
        aiInputField = rootView.findViewById(R.id.aiInputField)
        aiModeTabs = rootView.findViewById(R.id.aiModeTabs)

        // Initialize AI chips safely
        val chipIds = listOf(R.id.aiChip1, R.id.aiChip2, R.id.aiChip3, R.id.aiChip4, R.id.aiChip5)
        for (i in chipIds.indices) {
            aiChips[i] = rootView.findViewById(chipIds[i])
            aiChips[i]?.setOnClickListener {
                val text = aiChips[i]?.text?.toString() ?: ""
                if (text.isNotEmpty()) insertAISuggestion(text)
                performHapticFeedback()
            }
        }

        // Set up function bar buttons safely
        safeSetClick(rootView, R.id.btnAI) { toggleAIPanel("assistant") }
        safeSetClick(rootView, R.id.btnTranslate) { toggleAIPanel("translate") }
        safeSetClick(rootView, R.id.btnGrammar) { toggleAIPanel("grammar") }
        safeSetClick(rootView, R.id.btnSummarize) { toggleAIPanel("summarize") }
        safeSetClick(rootView, R.id.btnRewrite) { toggleAIPanel("rewrite") }
        safeSetClick(rootView, R.id.btnEmoji) { toggleAIPanel("emoji") }
        safeSetClick(rootView, R.id.btnSmartReply) { toggleAIPanel("smartreply") }
        safeSetClick(rootView, R.id.btnClipboard) { handleClipboard() }
        safeSetClick(rootView, R.id.btnCursorLeft) { moveCursor(-1) }
        safeSetClick(rootView, R.id.btnCursorRight) { moveCursor(1) }
        safeSetClick(rootView, R.id.btnCursorUp) { moveCursorLine(-1) }
        safeSetClick(rootView, R.id.btnCursorDown) { moveCursorLine(1) }
        safeSetClick(rootView, R.id.btnSelectAll) { selectAll() }
        safeSetClick(rootView, R.id.btnCut) { cutText() }
        safeSetClick(rootView, R.id.btnCopy) { copyText() }
        safeSetClick(rootView, R.id.btnPaste) { pasteText() }
        safeSetClick(rootView, R.id.btnSettings) { openSettings() }
        safeSetClick(rootView, R.id.btnCloseAI) { closeAIPanel() }
        safeSetClick(rootView, R.id.btnAISend) { processAIInput() }

        // Set up AI mode tabs
        val modes = listOf("assistant", "translate", "grammar", "summarize", "rewrite", "emoji")
        val tabIds = listOf(R.id.tabAssistant, R.id.tabTranslate, R.id.tabGrammar, R.id.tabSummarize, R.id.tabRewrite, R.id.tabEmoji)
        for (i in modes.indices) {
            val mode = modes[i]
            val btn = rootView.findViewById<Button>(tabIds[i])
            btn?.setOnClickListener {
                switchAIMode(mode)
                performHapticFeedback()
            }
        }

        // AI input field listener
        aiInputField?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                processAIInput()
                true
            } else {
                false
            }
        }

        // AI input field focus and text handling
        aiInputField?.setOnFocusChangeListener { _, hasFocus ->
            aiInputFieldHasFocus = hasFocus
            if (hasFocus) {
                aiInputField?.isCursorVisible = true
            }
        }

        aiInputField?.setOnClickListener {
            aiInputField?.requestFocus()
            aiInputFieldHasFocus = true
            // Hide soft keyboard to use our custom one
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(aiInputField?.windowToken, 0)
        }

        aiInputField?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                aiInputBuffer.clear()
                aiInputBuffer.append(s?.toString() ?: "")
            }
        })

        // Set up keyboard
        keyboard = Keyboard(this, R.xml.qwerty)
        kv?.keyboard = keyboard
        kv?.setOnKeyboardActionListener(this)
        kv?.isPreviewEnabled = true

        // Initialize suggestions
        updateAISuggestions("")

        return rootView
    }

    private fun safeSetClick(root: View, id: Int, action: () -> Unit) {
        try {
            root.findViewById<ImageButton>(id)?.setOnClickListener {
                try {
                    action()
                } catch (e: Exception) {
                    Toast.makeText(this, getString(R.string.ai_error, e.message), Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            // Ignore missing views
        }
    }

    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        caps = false
        isSymbols = false
        isBangla = false
        typingHistory.clear()
        currentWord = ""

        val inputType = editorInfo?.inputType ?: 0
        val variation = inputType and EditorInfo.TYPE_MASK_VARIATION
        autoCaps = variation != EditorInfo.TYPE_TEXT_VARIATION_PASSWORD &&
                   variation != EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD &&
                   variation != EditorInfo.TYPE_TEXT_VARIATION_URI &&
                   variation != EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        if (autoCaps) {
            caps = true
            keyboard?.setShifted(true)
            kv?.invalidateAllKeys()
        }
    }

    // ===== FUNCTION BAR HANDLERS =====

    private fun toggleAIPanel(mode: String) {
        try {
            if (aiPanel?.visibility == View.VISIBLE && currentAIMode == mode) {
                closeAIPanel()
                return
            }
            currentAIMode = mode
            aiPanel?.visibility = View.VISIBLE
            aiInputField?.requestFocus()
            aiInputFieldHasFocus = true
            switchAIMode(mode)
            performHapticFeedback()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.ai_error, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun closeAIPanel() {
        aiPanel?.visibility = View.GONE
        aiInputFieldHasFocus = false
        aiInputBuffer.clear()
        aiInputField?.text?.clear()
        performHapticFeedback()
    }

    private fun switchAIMode(mode: String) {
        try {
            currentAIMode = mode
            aiInputFieldHasFocus = true
            aiInputField?.requestFocus()

            val tabIds = mapOf(
                "assistant" to R.id.tabAssistant,
                "translate" to R.id.tabTranslate,
                "grammar" to R.id.tabGrammar,
                "summarize" to R.id.tabSummarize,
                "rewrite" to R.id.tabRewrite,
                "emoji" to R.id.tabEmoji
            )

            tabIds.forEach { (m, id) ->
                aiModeTabs?.findViewById<Button>(id)?.isSelected = (m == mode)
            }

            val titles = mapOf(
                "assistant" to R.string.ai_assistant,
                "translate" to R.string.ai_translate_label,
                "grammar" to R.string.ai_grammar_label,
                "summarize" to R.string.ai_summarize,
                "rewrite" to R.string.ai_rewrite,
                "emoji" to R.string.ai_emoji_label,
                "smartreply" to R.string.ai_smart_reply
            )
            aiPanelTitle?.setText(titles[mode] ?: R.string.ai_assistant)

            val placeholders = mapOf(
                "assistant" to R.string.ai_input_hint,
                "translate" to R.string.ai_input_hint,
                "grammar" to R.string.ai_input_hint,
                "summarize" to R.string.ai_input_hint,
                "rewrite" to R.string.ai_input_hint,
                "emoji" to R.string.ai_input_hint
            )
            aiInputField?.setHint(placeholders[mode] ?: R.string.ai_input_hint)

            generateAIContent(mode)
        } catch (e: Exception) {
            aiResultText?.text = getString(R.string.ai_error, e.message)
        }
    }

    private fun generateAIContent(mode: String) {
        try {
            val ic = currentInputConnection
            val textBeforeCursor = ic?.getTextBeforeCursor(500, 0)?.toString() ?: ""
            val fullText = typingHistory.joinToString(" ") + " " + textBeforeCursor

            val result = when (mode) {
                "assistant" -> generateAssistantSuggestions(fullText)
                "translate" -> generateTranslation(fullText)
                "grammar" -> checkGrammar(fullText)
                "summarize" -> summarizeText(fullText)
                "rewrite" -> rewriteText(fullText)
                "emoji" -> generateEmojiSuggestions(fullText)
                "smartreply" -> generateSmartReplies(fullText)
                else -> getString(R.string.ai_result_default)
            }

            aiResultText?.text = result
        } catch (e: Exception) {
            aiResultText?.text = getString(R.string.ai_error, e.message)
        }
    }

    private fun processAIInput() {
        try {
            val query = if (aiInputBuffer.isNotEmpty()) {
                aiInputBuffer.toString().trim()
            } else {
                aiInputField?.text?.toString()?.trim() ?: ""
            }

            if (query.isEmpty()) return

            val newResult = when (currentAIMode) {
                "translate" -> processTranslation(query)
                "grammar" -> processGrammar(query)
                "summarize" -> processSummarize(query)
                "rewrite" -> processRewrite(query)
                "emoji" -> processEmoji(query)
                else -> processAssistant(query)
            }

            aiResultText?.text = getString(R.string.ai_result_prefix, newResult)
            aiInputBuffer.clear()
            aiInputField?.text?.clear()
            performHapticFeedback()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.ai_error, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    // ===== AI GENERATION FUNCTIONS =====

    private fun generateAssistantSuggestions(context: String): String {
        if (context.isBlank()) {
            return "💡 Quick Start:\n• Start typing and I'll suggest completions\n• I can translate, check grammar, and more!\n• Try asking me to help with any text task"
        }

        val suggestions = getAISuggestions(context, currentWord)
        return if (suggestions.isNotEmpty()) {
            "💡 Smart Completions:\n${suggestions.joinToString("\n") { "• $it" }}"
        } else {
            "✅ No suggestions available. Keep typing!"
        }
    }

    private fun generateTranslation(text: String): String {
        if (text.isBlank()) {
            return "🌐 Supported Languages:\n🇺🇸 English → 🇧🇩 বাংলা\n🇺🇸 English → 🇪🇸 Español\n🇺🇸 English → 🇫🇷 Français\n🇺🇸 English → 🇩🇪 Deutsch\n🇺🇸 English → 🇯🇵 日本語\n\nType text and I'll translate it automatically!"
        }

        val lower = text.lowercase()
        val translations = mutableListOf<String>()

        translationDB.forEach { (key, langs) ->
            if (lower.contains(key)) {
                translations.add("🇧🇩 বাংলা: ${langs["bn"]}")
                translations.add("🇪🇸 Español: ${langs["es"]}")
                translations.add("🇫🇷 Français: ${langs["fr"]}")
                translations.add("🇩🇪 Deutsch: ${langs["de"]}")
                translations.add("🇯🇵 日本語: ${langs["jp"]}")
            }
        }

        return if (translations.isNotEmpty()) {
            "🌐 Translations:\n${translations.joinToString("\n")}"
        } else {
            "🌐 Quick Translations:\nবাংলা: হ্যালো (Hello)\nEspañol: Hola\nFrançais: Bonjour\nDeutsch: Hallo\n日本語: こんにちは"
        }
    }

    private fun checkGrammar(text: String): String {
        if (text.isBlank()) {
            return "📝 Grammar Rules Active:\n• Auto-capitalization: \"i\" → \"I\"\n• Contractions: \"dont\" → \"don't\"\n• Common misspellings will be flagged\n\nType to see real-time grammar checking!"
        }

        val corrections = mutableListOf<String>()

        grammarRules.forEach { (wrong, right) ->
            if (text.lowercase().contains(wrong) && !text.lowercase().contains(right.lowercase())) {
                corrections.add("✏️ \"$wrong\" → \"$right\"")
            }
        }

        if (text.matches(Regex("(^|\\.\\s+)[a-z].*"))) {
            corrections.add("🔤 Sentence should start with capital letter")
        }

        if (text.contains("  ")) {
            corrections.add("⚠️ Remove extra spaces")
        }

        return if (corrections.isNotEmpty()) {
            "✏️ Corrections Found:\n${corrections.joinToString("\n")}"
        } else {
            "✅ No Issues:\nYour text looks great! No grammar errors detected."
        }
    }

    private fun summarizeText(text: String): String {
        if (text.isBlank() || text.length < 50) {
            return "📋 How It Works:\n• I'll condense your text into key points\n• Type or paste longer text to summarize\n• Supports multiple paragraphs\n\nNeed at least 50 characters to summarize!"
        }

        val sentences = text.split(Regex("[.!?]+")).filter { it.trim().isNotEmpty() }
        val keySentences = sentences.take(3)

        return "📋 Summary:\n${keySentences.joinToString(". ")}."
    }

    private fun rewriteText(text: String): String {
        if (text.isBlank()) {
            return "🔄 Rewrite Styles:\n👔 Formal - Professional tone\n😎 Casual - Relaxed language\n😊 Friendly - Warm and approachable\n💼 Professional - Business-ready\n\nType text to see it rewritten!"
        }

        val results = mutableListOf<String>()
        tonePresets.forEach { (tone, replacements) ->
            var result = text
            replacements.forEach { (from, to) ->
                result = result.replace(Regex("\\b$from\\b", RegexOption.IGNORE_CASE), to)
            }
            val toneLabels = mapOf("formal" to "👔", "casual" to "😎", "friendly" to "😊", "professional" to "💼")
            results.add("${toneLabels[tone]} ${tone.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}: $result")
        }

        return "🔄 Rewritten Versions:\n${results.joinToString("\n\n")}"
    }

    private fun generateEmojiSuggestions(text: String): String {
        if (text.isBlank()) {
            return "😀 Popular Emoji:\n😊 Happy  😢 Sad  ❤️ Love  😡 Angry\n🤔 Thinking  🎉 Celebrate  💼 Work  🍕 Food\n☀️ Weather  ⏰ Time\n\nType mood keywords to get emoji!"
        }

        val lower = text.lowercase()
        val emojis = mutableListOf<String>()

        emojiDB.forEach { (keyword, emojiList) ->
            if (lower.contains(keyword)) {
                emojis.add("$keyword: $emojiList")
            }
        }

        return if (emojis.isNotEmpty()) {
            "😀 Emoji Suggestions:\n${emojis.joinToString("\n")}"
        } else {
            "😊 😄 ❤️ 👍 ✨ 💪 🎉 🌟\n(Type mood keywords like: happy, sad, love, work, food, weather)"
        }
    }

    private fun generateSmartReplies(context: String): String {
        val lower = context.lowercase()
        val replies = mutableListOf<String>()

        if (lower.contains("?")) {
            replies.addAll(listOf("Yes, definitely! 👍", "Sure thing! 😊", "Let me think about it... 🤔", "I'll get back to you soon"))
        }
        if (lower.contains("thank")) {
            replies.addAll(listOf("You're welcome! 😊", "No problem at all! 👍", "Happy to help! ✨"))
        }
        if (lower.contains("hello") || lower.contains("hi")) {
            replies.addAll(listOf("Hey! How are you? 😊", "Hi there! What's up? 👋", "Hello! Nice to hear from you! ✨"))
        }
        if (lower.contains("meeting") || lower.contains("schedule")) {
            replies.addAll(listOf("Sounds good, I'll be there! 📅", "What time works best? ⏰", "Let me check my calendar 🗓️"))
        }
        if (replies.isEmpty()) {
            replies.addAll(listOf("Got it! 👍", "Thanks for letting me know", "Sounds good! 😊", "I see, thanks! ✨"))
        }

        return "💬 Smart Replies:\n${replies.joinToString("\n")}"
    }

    // ===== AI PROCESSING FUNCTIONS =====

    private fun processTranslation(text: String): String {
        val lower = text.lowercase()
        val translations = mutableListOf<String>()

        translationDB.forEach { (key, langs) ->
            if (lower.contains(key)) {
                translations.add("🇧🇩 ${langs["bn"]}")
                translations.add("🇪🇸 ${langs["es"]}")
                translations.add("🇫🇷 ${langs["fr"]}")
                translations.add("🇩🇪 ${langs["de"]}")
                translations.add("🇯🇵 ${langs["jp"]}")
            }
        }

        return if (translations.isNotEmpty()) {
            translations.joinToString("\n")
        } else {
            "Translation (simulated):\n🇧🇩 বাংলা: [$text]\n🇪🇸 Español: [$text]\n🇫🇷 Français: [$text]"
        }
    }

    private fun processGrammar(text: String): String {
        val corrections = mutableListOf<String>()

        grammarRules.forEach { (wrong, right) ->
            if (text.lowercase().contains(wrong) && !text.lowercase().contains(right.lowercase())) {
                corrections.add("✏️ \"$wrong\" → \"$right\"")
            }
        }

        return if (corrections.isNotEmpty()) {
            corrections.joinToString("\n")
        } else {
            "✅ Your text looks perfect! No grammar issues detected."
        }
    }

    private fun processSummarize(text: String): String {
        if (text.length < 20) {
            return "Please provide more text to summarize. I need at least a few sentences to extract key points."
        }
        return summarizeText(text)
    }

    private fun processRewrite(text: String): String {
        if (text.length < 5) {
            return "Please provide more text to rewrite. I need at least a few words to work with."
        }

        val randomTone = tonePresets.keys.random()
        var result = text
        tonePresets[randomTone]?.forEach { (from, to) ->
            result = result.replace(Regex("\\b$from\\b", RegexOption.IGNORE_CASE), to)
        }

        return "$result\n\n(Rewritten in $randomTone tone)"
    }

    private fun processEmoji(query: String): String {
        val lower = query.lowercase()
        val emojis = mutableListOf<String>()

        emojiDB.forEach { (keyword, emojiList) ->
            if (lower.contains(keyword)) {
                emojis.add(emojiList)
            }
        }

        return if (emojis.isNotEmpty()) {
            emojis.joinToString(" ")
        } else {
            "😊 😄 ❤️ 👍 ✨ 💪 🎉 🌟 (Type mood keywords like: happy, sad, love, work, food, weather)"
        }
    }

    private fun processAssistant(query: String): String {
        val responses = listOf(
            "I can help with: text completion, translation, grammar checking, summarization, rewriting, and emoji suggestions.",
            "Try typing some text and I'll automatically suggest completions and improvements!",
            "I'm your AI keyboard assistant. Use the mode tabs above for specific tasks.",
            "I can translate to বাংলা, Español, Français, Deutsch, and 日本語!"
        )
        return responses.random()
    }

    // ===== UTILITY FUNCTIONS =====

    private fun getAISuggestions(context: String, currentWord: String): List<String> {
        val suggestions = mutableListOf<String>()

        if (currentWord.length > 2) {
            commonWords.filter { it.startsWith(currentWord) && it.length > currentWord.length }
                .take(2).forEach { suggestions.add(it) }

            aiSuggestions.keys.forEach { key ->
                if (key.startsWith(currentWord) && key != currentWord) {
                    suggestions.add(key)
                }
            }
        }

        val lowerContext = context.lowercase()
        aiSuggestions.forEach { (key, values) ->
            if (lowerContext.contains(key)) {
                suggestions.addAll(values.take(2))
            }
        }

        if (context.endsWith("?")) {
            suggestions.addAll(listOf("Yes, absolutely!", "Sure thing!", "Let me check..."))
        }

        return suggestions.distinct().take(5)
    }

    private fun updateAISuggestions(context: String) {
        try {
            val suggestions = getAISuggestions(context, currentWord)

            for (i in aiChips.indices) {
                val chip = aiChips[i]
                if (chip != null) {
                    if (i < suggestions.size) {
                        chip.text = suggestions[i]
                        chip.visibility = View.VISIBLE
                    } else {
                        chip.visibility = View.GONE
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore suggestion update errors
        }
    }

    private fun insertAISuggestion(text: String) {
        val ic = currentInputConnection ?: return

        if (currentWord.isNotEmpty()) {
            ic.deleteSurroundingText(currentWord.length, 0)
        }

        ic.commitText("$text ", 1)
        currentWord = ""
        typingHistory.add(text.lowercase())
        updateAISuggestions("")
    }

    // ===== CLIPBOARD & CURSOR FUNCTIONS =====

    private fun handleClipboard() {
        try {
            val clip = clipboardManager?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                Toast.makeText(this, getString(R.string.toast_clipboard_empty), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, getString(R.string.toast_clipboard_empty), Toast.LENGTH_SHORT).show()
            }
            performHapticFeedback()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.toast_clipboard_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveCursor(direction: Int) {
        val ic = currentInputConnection ?: return
        if (direction < 0) {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT))
        } else {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT))
        }
        performHapticFeedback()
    }

    private fun moveCursorLine(direction: Int) {
        val ic = currentInputConnection ?: return
        if (direction < 0) {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP))
        } else {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN))
        }
        performHapticFeedback()
    }

    private fun selectAll() {
        try {
            val ic = currentInputConnection ?: return
            val text = ic.getTextBeforeCursor(10000, 0) ?: return
            ic.setSelection(0, text.length)
            performHapticFeedback()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.toast_select_all_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun cutText() {
        try {
            val ic = currentInputConnection ?: return
            val selected = ic.getSelectedText(0)
            if (selected != null) {
                clipboardManager?.setPrimaryClip(ClipData.newPlainText(null, selected))
                ic.commitText("", 1)
                Toast.makeText(this, getString(R.string.toast_cut_success), Toast.LENGTH_SHORT).show()
            }
            performHapticFeedback()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.toast_cut_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyText() {
        try {
            val ic = currentInputConnection ?: return
            val selected = ic.getSelectedText(0)
            if (selected != null) {
                clipboardManager?.setPrimaryClip(ClipData.newPlainText(null, selected))
                Toast.makeText(this, getString(R.string.toast_copy_success), Toast.LENGTH_SHORT).show()
            }
            performHapticFeedback()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.toast_copy_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun pasteText() {
        try {
            val clip = clipboardManager?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                currentInputConnection?.commitText(clip.getItemAt(0).text, 1)
                Toast.makeText(this, getString(R.string.toast_paste_success), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.toast_clipboard_empty), Toast.LENGTH_SHORT).show()
            }
            performHapticFeedback()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.toast_paste_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSettings() {
        try {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            performHapticFeedback()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.toast_settings_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun performHapticFeedback() {
        try {
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(10)
                }
            }
        } catch (e: Exception) {
            // Ignore vibration errors
        }
    }

    // ===== KEYBOARD EVENT HANDLERS =====

    override fun onPress(primaryCode: Int) {
        if (vibrator?.hasVibrator() == true) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(15)
                }
            } catch (e: Exception) {}
        }
        try {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.1f)
        } catch (e: Exception) {}
    }

    override fun onRelease(primaryCode: Int) {}

    // Handle key input for AI text field
    private fun handleAIInputKey(primaryCode: Int) {
        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> {
                if (aiInputBuffer.isNotEmpty()) {
                    aiInputBuffer.deleteCharAt(aiInputBuffer.length - 1)
                    aiInputField?.setText(aiInputBuffer.toString())
                    aiInputField?.setSelection(aiInputBuffer.length)
                }
            }

            Keyboard.KEYCODE_DONE, -3 -> {
                // Enter key - process AI input
                processAIInput()
            }

            32 -> {
                // Space
                aiInputBuffer.append(' ')
                aiInputField?.setText(aiInputBuffer.toString())
                aiInputField?.setSelection(aiInputBuffer.length)
            }

            -1 -> {
                // Shift - toggle caps for AI input
                caps = !caps
                keyboard?.setShifted(caps)
                kv?.invalidateAllKeys()
            }

            -2, -6 -> {
                // Layout switch keys - ignore when typing in AI field
            }

            else -> {
                var code = primaryCode.toChar().toString()
                if (caps) {
                    code = code.uppercase()
                }
                aiInputBuffer.append(code)
                aiInputField?.setText(aiInputBuffer.toString())
                aiInputField?.setSelection(aiInputBuffer.length)
            }
        }
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        try {
            // If AI input field has focus, route input to it
            if (aiInputFieldHasFocus && aiPanel?.visibility == View.VISIBLE) {
                handleAIInputKey(primaryCode)
                return
            }

            val ic = currentInputConnection ?: return

            when (primaryCode) {
                Keyboard.KEYCODE_DELETE -> {
                    val selected = ic.getSelectedText(0)
                    if (selected == null) {
                        ic.deleteSurroundingText(1, 0)
                        if (currentWord.isNotEmpty()) {
                            currentWord = currentWord.dropLast(1)
                        }
                    } else {
                        ic.commitText("", 1)
                    }
                }

                Keyboard.KEYCODE_SHIFT -> {
                    isShiftLocked = caps
                    caps = !caps
                    keyboard?.setShifted(caps)
                    kv?.invalidateAllKeys()
                }

                Keyboard.KEYCODE_DONE -> {
                    requestHideSelf(0)
                }

                -2 -> {
                    if (!isSymbols) {
                        isSymbols = true
                        keyboard = Keyboard(this, R.xml.symbols)
                        kv?.keyboard = keyboard
                        kv?.invalidateAllKeys()
                    } else {
                        isSymbols = false
                        keyboard = Keyboard(this, R.xml.qwerty)
                        kv?.keyboard = keyboard
                        keyboard?.setShifted(caps)
                        kv?.invalidateAllKeys()
                    }
                }

                -6 -> {
                    if (!isBangla) {
                        isBangla = true
                        isSymbols = false
                        keyboard = Keyboard(this, R.xml.bangla)
                        kv?.keyboard = keyboard
                        kv?.invalidateAllKeys()
                    } else {
                        isBangla = false
                        keyboard = Keyboard(this, R.xml.qwerty)
                        kv?.keyboard = keyboard
                        keyboard?.setShifted(caps)
                        kv?.invalidateAllKeys()
                    }
                }

                else -> {
                    var code = primaryCode.toChar().toString()
                    if (caps && !isSymbols && !isBangla) {
                        code = code.uppercase()
                    }
                    ic.commitText(code, 1)

                    if (primaryCode in 97..122 || primaryCode in 65..90) {
                        currentWord += code.lowercase()
                    } else if (primaryCode == 32) {
                        if (currentWord.isNotEmpty()) {
                            typingHistory.add(currentWord)
                            currentWord = ""
                        }
                    }

                    if (primaryCode == 32) {
                        if (autoCaps && !lastSpaceWasCaps && !isBangla) {
                            caps = true
                            keyboard?.setShifted(true)
                            kv?.invalidateAllKeys()
                            lastSpaceWasCaps = true
                        }
                    } else {
                        lastSpaceWasCaps = false
                        if (caps && autoCaps && !isShiftLocked && !isBangla) {
                            caps = false
                            keyboard?.setShifted(false)
                            kv?.invalidateAllKeys()
                        }
                    }

                    // Update AI suggestions
                    updateAISuggestions(typingHistory.joinToString(" "))
                }
            }
        } catch (e: Exception) {
            // Prevent keyboard crashes
        }
    }

    override fun onText(text: CharSequence?) {
        currentInputConnection?.commitText(text, 1)
    }

    override fun swipeLeft() {
        moveCursor(-1)
    }

    override fun swipeRight() {
        moveCursor(1)
    }

    override fun swipeDown() {
        requestHideSelf(0)
    }

    override fun swipeUp() {
        pasteText()
    }
}
