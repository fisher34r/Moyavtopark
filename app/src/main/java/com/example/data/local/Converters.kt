package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.fleet.DocumentType
import com.example.data.fleet.DriverStatus
import com.example.data.fleet.ServiceType
import com.example.data.fleet.VehicleStatus
import com.example.data.fleet.VehicleType
import com.example.data.fleet.WaybillStatus
import com.example.data.model.RateType
import com.example.data.model.TripStatus

class Converters {
    @TypeConverter
    fun fromRateType(value: RateType): String = value.name

    @TypeConverter
    fun toRateType(value: String): RateType = try {
        RateType.valueOf(value)
    } catch (_: Exception) {
        RateType.PER_TON_KM
    }

    @TypeConverter
    fun fromTripStatus(value: TripStatus): String = value.name

    @TypeConverter
    fun toTripStatus(value: String): TripStatus = try {
        TripStatus.valueOf(value)
    } catch (_: Exception) {
        TripStatus.IN_TRANSIT
    }

    @TypeConverter
    fun fromVehicleType(value: VehicleType): String = value.name

    @TypeConverter
    fun toVehicleType(value: String): VehicleType = try {
        VehicleType.valueOf(value)
    } catch (_: Exception) {
        VehicleType.TRACTOR
    }

    @TypeConverter
    fun fromVehicleStatus(value: VehicleStatus): String = value.name

    @TypeConverter
    fun toVehicleStatus(value: String): VehicleStatus = try {
        VehicleStatus.valueOf(value)
    } catch (_: Exception) {
        VehicleStatus.AVAILABLE
    }

    @TypeConverter
    fun fromDriverStatus(value: DriverStatus): String = value.name

    @TypeConverter
    fun toDriverStatus(value: String): DriverStatus = try {
        DriverStatus.valueOf(value)
    } catch (_: Exception) {
        DriverStatus.AVAILABLE
    }

    @TypeConverter
    fun fromServiceType(value: ServiceType): String = value.name

    @TypeConverter
    fun toServiceType(value: String): ServiceType = try {
        ServiceType.valueOf(value)
    } catch (_: Exception) {
        ServiceType.TO_1
    }

    @TypeConverter
    fun fromWaybillStatus(value: WaybillStatus): String = value.name

    @TypeConverter
    fun toWaybillStatus(value: String): WaybillStatus = try {
        WaybillStatus.valueOf(value)
    } catch (_: Exception) {
        WaybillStatus.ISSUED
    }

    @TypeConverter
    fun fromDocumentType(value: DocumentType): String = value.name

    @TypeConverter
    fun toDocumentType(value: String): DocumentType = try {
        DocumentType.valueOf(value)
    } catch (_: Exception) {
        DocumentType.OSAGO
    }
}
