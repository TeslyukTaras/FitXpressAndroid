package com.hexis.bi.utils.constants

/**
 * Firestore schema for suit orders.
 *
 * ```
 * users/{uid}/orders/{orderId}
 *   orderNumber: string                       // human-readable, copyable on Order Details
 *   status: string                            // OrderStatus enum name
 *   statusHistory: [{ status: string, at: Timestamp }]
 *   createdAt / updatedAt: Timestamp
 *   eta: Timestamp?                           // set when known (production/shipping)
 *   trackingNumber: string?                   // set when shipped
 *   carrier: string?
 *   suitId: string?                           // set at post-delivery activation
 *   scanId: string?                           // ref to users/{uid}/scans; provenance of the sizing
 *   suitSize: string
 *   heightCm / weightKg: number               // values confirmed on "Your size results"
 *   contact: { firstName, lastName, email, phoneCountryIso, phoneDialCode, phoneNumber }
 *   shippingAddress: { countryIso, countryName, company, addressLine, apartment,
 *                      city, region, postalCode, note }
 * ```
 *
 * Contact and address are snapshots taken at order time (not references to the
 * profile), so later profile edits never rewrite past orders. A separate
 * top-level registry of valid suit IDs is planned for activation validation;
 * it is not part of this schema yet.
 */
object OrderFirestoreConstants {
    const val COLLECTION_ORDERS = "orders"

    const val FIELD_ORDER_NUMBER = "orderNumber"
    const val FIELD_STATUS = "status"
    const val FIELD_STATUS_HISTORY = "statusHistory"
    const val FIELD_STATUS_EVENT_STATUS = "status"
    const val FIELD_STATUS_EVENT_AT = "at"
    const val FIELD_CREATED_AT = "createdAt"
    const val FIELD_UPDATED_AT = "updatedAt"
    const val FIELD_ETA = "eta"
    const val FIELD_TRACKING_NUMBER = "trackingNumber"
    const val FIELD_CARRIER = "carrier"
    const val FIELD_SUIT_ID = "suitId"
    const val FIELD_SCAN_ID = "scanId"
    const val FIELD_SUIT_SIZE = "suitSize"
    const val FIELD_HEIGHT_CM = "heightCm"
    const val FIELD_WEIGHT_KG = "weightKg"

    const val FIELD_CONTACT = "contact"
    const val FIELD_FIRST_NAME = "firstName"
    const val FIELD_LAST_NAME = "lastName"
    const val FIELD_EMAIL = "email"
    const val FIELD_PHONE_COUNTRY_ISO = "phoneCountryIso"
    const val FIELD_PHONE_DIAL_CODE = "phoneDialCode"
    const val FIELD_PHONE_NUMBER = "phoneNumber"

    const val FIELD_SHIPPING_ADDRESS = "shippingAddress"
    const val FIELD_COUNTRY_ISO = "countryIso"
    const val FIELD_COUNTRY_NAME = "countryName"
    const val FIELD_COMPANY = "company"
    const val FIELD_ADDRESS_LINE = "addressLine"
    const val FIELD_APARTMENT = "apartment"
    const val FIELD_CITY = "city"
    const val FIELD_REGION = "region"
    const val FIELD_POSTAL_CODE = "postalCode"
    const val FIELD_NOTE = "note"

    const val ORDER_NUMBER_PREFIX = "HX-"
    const val ORDER_NUMBER_ID_LENGTH = 8

    /** How many recent orders to scan client-side for the latest active one. */
    const val LATEST_ORDER_LOOKBACK = 5L
}
