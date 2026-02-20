package com.rudy.expensetracker.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Coronavirus
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nightlife
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.RealEstateAgent
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.PedalBike
import androidx.compose.ui.graphics.vector.ImageVector

object IconManager {
    private val predefinedIcons = mapOf(
        // Financial
        "credit_card" to Icons.Default.CreditCard,
        "account_balance" to Icons.Default.AccountBalance,
        "local_atm" to Icons.Default.LocalAtm,
        "payment" to Icons.Default.Payment,
        "savings" to Icons.Default.Savings,
        "account_balance_wallet" to Icons.Default.AccountBalanceWallet,
        "request_quote" to Icons.Default.RequestQuote,
        "attach_money" to Icons.Default.AttachMoney,
        "money_off" to Icons.Default.MoneyOff,
        "monetization_on" to Icons.Default.MonetizationOn,

        // Transportation
        "directions_car" to Icons.Default.DirectionsCar,
        "local_gas_station" to Icons.Default.LocalGasStation,
        "flight" to Icons.Default.Flight,
        "train" to Icons.Default.Train,
        "directions_bus" to Icons.Default.DirectionsBus,
        "two_wheeler" to Icons.Default.TwoWheeler,
        "local_taxi" to Icons.Default.LocalTaxi,
        "sailing" to Icons.Default.Sailing,
        "pedal_bike" to Icons.Outlined.PedalBike, // Fixed

        // Food & Dining
        "restaurant" to Icons.Default.Restaurant,
        "shopping_cart" to Icons.Default.ShoppingCart,
        "local_cafe" to Icons.Default.LocalCafe,
        "fastfood" to Icons.Default.Fastfood,
        "local_pizza" to Icons.Default.LocalPizza,
        "local_bar" to Icons.Default.LocalBar,
        "icecream" to Icons.Default.Icecream,
        "cake" to Icons.Default.Cake,

        // Entertainment
        "movie" to Icons.Default.Movie,
        "music_note" to Icons.Default.MusicNote,
        "sports_esports" to Icons.Default.SportsEsports,
        "theater_comedy" to Icons.Default.TheaterComedy,
        "sports_soccer" to Icons.Default.SportsSoccer,
        "casino" to Icons.Default.Casino,
        "celebration" to Icons.Default.Celebration,
        "nightlife" to Icons.Default.Nightlife,

        // Health & Fitness
        "local_hospital" to Icons.Default.LocalHospital,
        "fitness_center" to Icons.Default.FitnessCenter,
        "healing" to Icons.Default.Healing,
        "local_pharmacy" to Icons.Default.LocalPharmacy,
        "favorite" to Icons.Default.Favorite,
        "medical_services" to Icons.Default.MedicalServices,
        "coronavirus" to Icons.Default.Coronavirus,

        // Home & Living
        "home" to Icons.Default.Home,
        "electric_bolt" to Icons.Default.ElectricBolt,
        "water_drop" to Icons.Default.WaterDrop,
        "wifi" to Icons.Default.Wifi,
        "cleaning_services" to Icons.Default.CleaningServices,
        "checkroom" to Icons.Default.Checkroom,
        "bed" to Icons.Default.Bed,
        "kitchen" to Icons.Default.Kitchen,

        // Personal & Others
        "shopping_bag" to Icons.Default.ShoppingBag,
        "school" to Icons.Default.School,
        "work" to Icons.Default.Work,
        "phone" to Icons.Default.Phone,
        "pets" to Icons.Default.Pets,
        "category" to Icons.Default.Category,
        "child_care" to Icons.Default.ChildCare,
        "spa" to Icons.Default.Spa,
        "emoji_people" to Icons.Default.EmojiPeople,
        "face" to Icons.Default.Face,

        // Investment
        "trending_up" to Icons.Default.TrendingUp,
        "show_chart" to Icons.Default.ShowChart,
        "pie_chart" to Icons.Default.PieChart,
        "insights" to Icons.Default.Insights,
        "real_estate_agent" to Icons.Default.RealEstateAgent,
        "bar_chart" to Icons.Default.BarChart,

        // Income
        "attach_money" to Icons.Default.AttachMoney,
        "monetization_on" to Icons.Default.MonetizationOn,
        "paid" to Icons.Default.Paid,
        "request_quote" to Icons.Default.RequestQuote,
        "work" to Icons.Default.Work,
        "card_giftcard" to Icons.Default.CardGiftcard,
        "redeem" to Icons.Default.Redeem
    )

    private val reverseIcons: Map<ImageVector, String> =
        predefinedIcons.entries.associate { (name, icon) -> icon to name }

    fun getIconByName(iconName: String): ImageVector {
        return predefinedIcons[iconName] ?: Icons.Default.Category
    }

    fun getNameByIcon(icon: ImageVector): String {
        return reverseIcons[icon] ?: "category" // fallback
    }

    fun getIconsByCategory(): Map<String, List<Pair<String, ImageVector>>> {
        return mapOf(
            "Financial" to predefinedIcons.filterKeys {
                it in listOf("credit_card", "account_balance", "local_atm", "payment", "savings", "account_balance_wallet", "request_quote", "attach_money", "money_off", "monetization_on")
            }.toList(),
            "Transport" to predefinedIcons.filterKeys {
                it in listOf("directions_car", "local_gas_station", "flight", "train", "directions_bus", "two_wheeler", "local_taxi", "sailing", "pedal_bike")
            }.toList(),
            "Food" to predefinedIcons.filterKeys {
                it in listOf("restaurant", "shopping_cart", "local_cafe", "fastfood", "local_pizza", "local_bar", "icecream", "cake")
            }.toList(),
            "Entertainment" to predefinedIcons.filterKeys {
                it in listOf("movie", "music_note", "sports_esports", "theater_comedy", "sports_soccer", "casino", "celebration", "nightlife")
            }.toList(),
            "Health" to predefinedIcons.filterKeys {
                it in listOf("local_hospital", "fitness_center", "healing", "local_pharmacy", "favorite", "medical_services", "coronavirus")
            }.toList(),
            "Home" to predefinedIcons.filterKeys {
                it in listOf("home", "electric_bolt", "water_drop", "wifi", "cleaning_services", "checkroom", "bed", "kitchen")
            }.toList(),
            "Personal" to predefinedIcons.filterKeys {
                it in listOf("shopping_bag", "school", "work", "phone", "pets", "child_care", "spa", "emoji_people", "face")
            }.toList(),
            "Investment" to predefinedIcons.filterKeys {
                it in listOf("trending_up", "show_chart", "pie_chart", "insights", "real_estate_agent", "bar_chart")
            }.toList(),
            "Income" to predefinedIcons.filterKeys {
                it in listOf("attach_money", "monetization_on", "paid", "request_quote", "work", "card_giftcard", "redeem")
            }.toList()
        )
    }
}
