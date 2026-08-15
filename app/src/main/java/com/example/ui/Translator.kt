package com.example.ui

object Translator {
    var currentLanguage: String = "English"

    private val hindiMap = mapOf(
        "App setting" to "ऐप सेटिंग",
        "Develop by Naman" to "नमन द्वारा विकसित",
        "Manage Elections" to "चुनाव प्रबंधित करें",
        "Version: 1.0.0 Pro\nBuild: 2024\nDeveloped in Kotlin & Jetpack Compose" to "संस्करण: 1.0.0 प्रो\nबिल्ड: 2024\nकोटलिन और जेटपैक कंपोज़ में विकसित",
        "You have successfully cast your vote.\nYou cannot vote again." to "आपने सफलतापूर्वक अपना वोट डाल दिया है।\nआप दोबारा वोट नहीं कर सकते।",
        "Logo" to "प्रतीक चिन्ह",
        "Voting Statistics & Map" to "मतदान के आंकड़े और मानचित्र",
        "LIVE NOW" to "अभी लाइव",
        "Set a password to protect the election results from unauthorized access." to "चुनाव परिणामों को अनधिकृत पहुंच से बचाने के लिए पासवर्ड सेट करें।",
        "Are you sure you want to delete this election?" to "क्या आप वाकई इस चुनाव को हटाना चाहते हैं?",
        "Language" to "भाषा",
        "admin@eduvote.com" to "admin@eduvote.com",
        "Which session/class are you voting in?" to "आप किस सत्र/कक्षा में मतदान कर रहे हैं?",
        "Select your candidate" to "अपना उम्मीदवार चुनें",
        "Party Logo" to "पार्टी लोगो",
        "Set Results Password" to "परिणाम पासवर्ड सेट करें",
        "History" to "इतिहास",
        "Election History" to "चुनाव इतिहास",
        "Save" to "सहेजें",
        "Home" to "होम",
        "Enter a name for your avatar" to "अपने अवतार के लिए एक नाम दर्ज करें",
        "Delete" to "हटाएं",
        "Remove Lock" to "लॉक हटाएं",
        "Edit Election Name" to "चुनाव का नाम संपादित करें",
        "Create New Election" to "नया चुनाव बनाएं",
        "Current Standings" to "वर्तमान स्थिति",
        "Delete Election" to "चुनाव हटाएं",
        "Enable Notifications" to "सूचनाएं सक्षम करें",
        "Results" to "परिणाम",
        "Create Election" to "चुनाव बनाएं",
        "No Results Yet" to "अभी कोई परिणाम नहीं",
        "Vote Cast Successfully!" to "वोट सफलतापूर्वक डाला गया!",
        "Unlock" to "अनलॉक",
        "Recent Elections" to "हाल के चुनाव",
        "View completed and deleted elections" to "पूरे हुए और हटाए गए चुनाव देखें",
        "Complete" to "पूरा",
        "Candidate Details" to "उम्मीदवार का विवरण",
        "Voter Login" to "मतदाता लॉगिन",
        "Photo" to "तस्वीर",
        "Add Students" to "छात्रों को जोड़ें",
        "Session Details" to "सत्र विवरण",
        "All candidates added!" to "सभी उम्मीदवार जोड़े गए!",
        "Control and monitor all active events" to "सभी सक्रिय घटनाओं को नियंत्रित और मॉनिटर करें",
        "Real Photo" to "असली तस्वीर",
        "No active sessions available." to "कोई सक्रिय सत्र उपलब्ध नहीं है।",
        "Password" to "पासवर्ड",
        "Cancel" to "रद्द करें",
        "Voting Complete" to "मतदान पूरा हुआ",
        "Voting Booth" to "मतदान केंद्र",
        "Avatar Image Seed" to "अवतार छवि सीड",
        "Election Title" to "चुनाव का शीर्षक",
        "Terms and conditions" to "नियम और शर्तें",
        "Election Type" to "चुनाव का प्रकार",
        "Admin Dashboard" to "व्यवस्थापक डैशबोर्ड",
        "OK" to "ठीक है",
        "No ongoing elections" to "कोई चल रहा चुनाव नहीं",
        "App Settings" to "ऐप सेटिंग्स",
        "App UI Design & Theme" to "ऐप UI डिज़ाइन और थीम",
        "Select a custom layout theme. This will instantly change the appearance of the dashboard and voting screens." to "कस्टम लेआउट थीम चुनें। यह तुरंत डैशबोर्ड और वोटिंग स्क्रीन का रूप बदल देगा।",
        "No recent elections" to "कोई हाल का चुनाव नहीं",
        "COMPLETED" to "पूरा हुआ",
        "Number of Candidates (2-4)" to "उम्मीदवारों की संख्या (2-4)",
        "Candidate Name" to "उम्मीदवार का नाम",
        "Enter your password to view the results." to "परिणाम देखने के लिए अपना पासवर्ड दर्ज करें।",
        "No elections created yet." to "अभी तक कोई चुनाव नहीं बनाया गया।",
        "Thank you for voting." to "मतदान के लिए धन्यवाद।",
        "Hindi" to "हिंदी",
        "Create Session" to "सत्र बनाएं",
        "Vote Session" to "मतदान सत्र",
        "Class" to "कक्षा",
        "English" to "अंग्रेजी",
        "No history available." to "कोई इतिहास उपलब्ध नहीं।",
        "App Specifications" to "ऐप विनिर्देश",
        "Confirm Password" to "पासवर्ड की पुष्टि करें",
        "Results & Analytics" to "परिणाम और विश्लेषिकी",
        "Add Candidates" to "उम्मीदवार जोड़ें",
        "Return to Login" to "लॉगिन पर लौटें",
        "Loading candidates or no candidates found..." to "उम्मीदवार लोड हो रहे हैं या कोई उम्मीदवार नहीं मिला...",
        "Elections" to "चुनाव",
        "Voters" to "मतदाता",
        "Finish" to "समाप्त",
        "Results Locked" to "परिणाम लॉक हैं",
        "Once an election is completed, the results and analytics will appear here." to "एक बार चुनाव पूरा हो जाने पर, परिणाम और विश्लेषिकी यहां दिखाई देंगे।",
        "DELETED" to "हटाया गया",
        "Results are currently unprotected. Tap the unlock icon to set a password." to "परिणाम वर्तमान में असुरक्षित हैं। पासवर्ड सेट करने के लिए अनलॉक आइकन टैप करें।"
    )

    fun tr(text: String): String {
        if (currentLanguage == "Hindi") {
            // Check exact match
            val translated = hindiMap[text]
            if (translated != null) return translated
            
            // Check substrings for dynamic text like "Winner of $electionTitle"
            if (text.startsWith("Winner of ")) {
                return "विजेता: " + text.removePrefix("Winner of ")
            }
            if (text.endsWith(" students added!")) {
                return text.removeSuffix(" students added!") + " छात्र जोड़े गए!"
            }
            if (text.endsWith(" Votes Received")) {
                return text.removeSuffix(" Votes Received") + " वोट मिले"
            }
            if (text.endsWith(" Votes")) {
                return text.removeSuffix(" Votes") + " वोट"
            }
        }
        return text
    }
}
