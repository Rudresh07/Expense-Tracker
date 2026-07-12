package com.rudy.expensetracker.sms

import android.util.Log

/**
 * Maps SMS content (merchant name + body) to one of the app's default category names:
 * Food, Transport, Shopping, Bills, Entertainment, Health, Education, Other
 */
object SmsCategorizer {

    private const val TAG = "SmsCategorizer"

    private val CATEGORY_KEYWORDS: Map<String, List<String>> = mapOf(

        "Food" to listOf(
            "swiggy", "zomato", "domino", "pizza", "mcdonald", "kfc", "subway",
            "burger king", "dunkin", "starbucks", "cafe", "restaurant", "food",
            "blinkit", "bigbasket", "grofers", "zepto", "dmart", "reliance fresh",
            "more supermarket", "haldiram", "faasos", "box8", "eatfit", "freshmenu",
            "behrouz", "milkbasket", "supr daily", "country delight", "grocery"
        ),

        "Transport" to listOf(
            "uber", "ola", "rapido", "namma yatri", "metro", "irctc", "railway",
            "redbus", "indigo", "spicejet", "goair", "vistara", "air india",
            "airport", "fastag", "petrol", "fuel", "shell", "hpcl", "iocl",
            "bharat petroleum", "parking", "toll",
            "makemytrip", "yatra", "cleartrip", "easemytrip", "goibibo"
        ),

        "Shopping" to listOf(
            "amazon", "flipkart", "myntra", "meesho", "snapdeal", "nykaa",
            "ajio", "shopsy", "tata cliq", "jabong", "limeroad", "purplle",
            "firstcry", "pepperfry", "urban ladder", "ikea", "croma",
            "reliance digital", "vijay sales", "decathlon", "zara", "h&m",
            "lifestyle", "pantaloons", "westside", "max fashion", "shoppers stop",
            "big bazaar"
        ),

        "Bills" to listOf(
            "electricity", "jio", "airtel", "vodafone", "bsnl", "idea",
            "recharge", "broadband", "internet", "dth", "tata sky", "dish tv",
            "sun direct", "d2h", "water bill", "lpg", "cylinder", "indane",
            "hp gas", "bharat gas", "postpaid", "utility", "emi", "loan",
            "insurance", "lic", "rent", "maintenance", "society",
            "bescom", "tata power", "adani electricity", "msedcl", "bses",
            "tneb", "kseb"
        ),

        "Entertainment" to listOf(
            "netflix", "spotify", "amazon prime", "hotstar", "disney",
            "youtube premium", "zee5", "sonyliv", "jiosaavn", "gaana", "wynk",
            "bookmyshow", "book my show", "pvr", "inox", "movie", "concert",
            "jiohotstar", "mxplayer", "voot", "alt balaji", "lionsgate"
        ),

        "Health" to listOf(
            "hospital", "clinic", "pharmacy", "medical", "apollo", "fortis",
            "max hospital", "manipal", "medanta", "narayana", "medicine",
            "chemist", "doctor", "lab", "pathology", "diagnostic", "netmeds",
            "1mg", "pharmeasy", "medlife", "healthkart", "cult.fit", "cure.fit",
            "practo", "thyrocare", "dr lal"
        ),

        "Education" to listOf(
            "school", "college", "university", "tuition", "udemy", "coursera",
            "byju", "unacademy", "vedantu", "simplilearn", "upgrad",
            "great learning", "whitehat jr", "toppr", "doubtnut", "duolingo",
            "chegg", "book fee", "exam fee"
        )
    )

    /**
     * Returns the best matching category name, or "Other" if nothing matches.
     * @param merchant  parsed merchant/payee name (can be null)
     * @param body      raw SMS body text
     */
    fun categorize(merchant: String?, body: String): String {
        val searchText = "${merchant.orEmpty()} $body".lowercase()
        Log.d(TAG, "categorize() — merchant='$merchant', searchText='${searchText.take(100)}'")

        for ((category, keywords) in CATEGORY_KEYWORDS) {
            val matched = keywords.firstOrNull { keyword -> searchText.contains(keyword) }
            if (matched != null) {
                Log.d(TAG, "Matched category='$category' via keyword='$matched'")
                return category
            }
        }

        Log.w(TAG, "No keyword matched — falling back to 'Other'. merchant='$merchant', body='${body.take(100)}'")
        return "Other"
    }
}