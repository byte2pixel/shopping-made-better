import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItemDetailsDto(
    @SerialName("id") val id: String?,
    @SerialName("title") val title : String?,
    //Brand of the item
    @SerialName("brand") val brand : String?,
    @SerialName("description") val description : String?,
    @SerialName("package_sizing") val sizing : String?,
    //holds a url for use in displaying the image
    @SerialName("image_url") val image : String?,
    @SerialName("source_link") val source : String?,
    //Expiration date
    @SerialName("shelf_life_days") val shelfLife : Int?,
    //Holds the shelf life category useful for showing what section the item might be stored in
    @SerialName("shelf_life_category") val lifeCategory : String?
        )
