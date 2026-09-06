package com.fullsail.shoppingmadebetter.feature.shoppinglists.domain


data class ItemDetails(
    val id: String?,
    val title : String?,
    val brand : String?,
    val description : String?,
    val sizing : String?,
    val image : String?,
    val source : String?,
    val shelfLife : Int?,
    val lifeCategory : String?
)