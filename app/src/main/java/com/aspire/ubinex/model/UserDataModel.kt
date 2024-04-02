package com.aspire.ubinex.model

class UserDataModel(
    var uid: String?,
    var name: String?,
    var phoneNumber: String?,
    var profileImage: String?,
    var gender : String?,
    var email : String?
) {
    constructor() : this(null, null, null, null, null, null)

}