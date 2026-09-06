import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItemDetailsDto(
    @SerialName("id") val id: String?,
    @SerialName("title") val title : String?,
    @SerialName("brand") val brand : String?,
    @SerialName("description") val description : String?,
    @SerialName("package_sizing") val sizing : String?,
    @SerialName("image_url") val image : String?,
    @SerialName("source_link") val source : String?,
    @SerialName("shelf_life_days") val shelfLife : Int?,
    @SerialName("shelf_life_category") val lifeCategory : String?
        )
