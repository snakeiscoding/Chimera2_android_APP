package com.jht.enums



/** Indicates what kind of measurement */
enum class UnitQuantity {
    Length,
    Time,
    Velocity,
    Mass,
    //Power, // we only really use Watts
    //Energy, // we only really use nutrition Calories (or kcal)
}

/** Indicates a specific unit of measurement. Internal stores a ratio of the base scientific unit for the quantity (derived * ratio = base). */
enum class UnitMeasurement(val quantity: UnitQuantity, val ratio: Double) { // would probably need to add int offset for temperature?
    // Conversions from https://www.govinfo.gov/content/pkg/GOVPUB-C13-f10c2ff9e7af2091314396a2d53213e4/pdf/GOVPUB-C13-f10c2ff9e7af2091314396a2d53213e4.pdf
    // See also https://www.nist.gov/pml/special-publication-811/nist-guide-si-appendix-b-conversion-factors/nist-guide-si-appendix-b8

    // Do we consider percentage as a measurement? Any need to distinguish simple number vs simple ratio?
    //Number(UnitQuantity.None, 1.0),
    //Percentage(UnitQuantity.None, 0.01),

    // Base length is meter
    Millimeter(UnitQuantity.Length, 0.001),
    Centimeter(UnitQuantity.Length, 0.01),
    Meter(UnitQuantity.Length, 1.0),
    Kilometer(UnitQuantity.Length, 1000.0),
    Inch(UnitQuantity.Length, 2.54 * Centimeter.ratio),
    Foot(UnitQuantity.Length, 0.3048),
    Yard(UnitQuantity.Length, 0.9144),
    Mile(UnitQuantity.Length, 1.609344 * Kilometer.ratio),

    // Base time is second
    Millisecond(UnitQuantity.Time, 0.001),
    Second(UnitQuantity.Time, 1.0),
    Minute(UnitQuantity.Time, 60.0),
    Hour(UnitQuantity.Time, 3600.0),
    Day(UnitQuantity.Time, 86400.0),
    // Perhaps better to treat age as a number instead of measurement? conversions vary
    //Year(UnitQuantity.Time, 31557600.0), // "One Julian astronomical year, has 365.25 days"

    // Velocity is meter / second
    MeterPerSecond(UnitQuantity.Velocity, 1.0),
    KilometerPerHour(UnitQuantity.Velocity, Kilometer.ratio / Hour.ratio),
    MilePerHour(UnitQuantity.Velocity, Mile.ratio / Hour.ratio),

    // Mass is grams (weight is more of a force)
    Gram(UnitQuantity.Mass, 1.0),
    Kilogram(UnitQuantity.Mass, 1000.0),
    Pound(UnitQuantity.Mass, 0.45359237 * Kilogram.ratio),

    // Base power is Watt
    //Watt(UnitQuantity.Power, 1.0),

    // Base energy is Joule
    //Joule(UnitQuantity.Energy, 1.0),
    //Calorie(UnitQuantity.Energy, 4.184), // physics calorie - "All use of the calorie is deprecated"? - let's omit this one to avoid confusion, we really just use kcal
    //Kilocalorie(UnitQuantity.Energy, 4184.0), // "The calorie used in nutrition is the same as the thermochemical kilocalorie or kcal."

    ; // END LIST
}
