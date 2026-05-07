package iq.gov.smartkarbala.data.repository

import iq.gov.smartkarbala.data.model.Place
import iq.gov.smartkarbala.data.model.PlaceCategory

object KarbalaPlacesData {

    fun getKarbalaPlaces(): List<Place> = listOf(

        // ── MOSQUES & SHRINES ────────────────────────────────
        Place(
            id = "shrine_hussain",
            name = "Al-Hussein Shrine",
            nameArabic = "مرقد الإمام الحسين ع",
            category = PlaceCategory.MOSQUE,
            description = "أقدس البقاع في كربلاء المقدسة، مرقد سيد الشهداء الإمام الحسين بن علي (ع)",
            latitude = 32.6161, longitude = 44.0326,
            address = "وسط كربلاء المقدسة، قرب الحرمين",
            rating = 5.0f, reviewCount = 150000,
            openingHours = "24 ساعة",
            isFeatured = true, isOpen = true
        ),
        Place(
            id = "shrine_abbas",
            name = "Al-Abbas Shrine",
            nameArabic = "مرقد أبي الفضل العباس ع",
            category = PlaceCategory.MOSQUE,
            description = "حرم العباس بن علي (ع)، قمر بني هاشم، حامل لواء الإمام الحسين (ع)",
            latitude = 32.6175, longitude = 44.0322,
            address = "مقابل مرقد الإمام الحسين (ع)",
            rating = 5.0f, reviewCount = 120000,
            openingHours = "24 ساعة",
            isFeatured = true, isOpen = true
        ),

        // ── HOTELS ──────────────────────────────────────────
        Place(
            id = "hotel_hayat",
            name = "Hayat Karbala Hotel",
            nameArabic = "فندق حياة كربلاء",
            category = PlaceCategory.HOTEL,
            description = "فندق 5 نجوم بقرب الحرمين الشريفين، مطعم دولي، صالة رياضية",
            latitude = 32.6140, longitude = 44.0280,
            address = "شارع بابل، كربلاء المقدسة",
            phone = "07801234567",
            rating = 4.5f, reviewCount = 3200,
            priceRange = "150,000 - 400,000 د.ع",
            openingHours = "24 ساعة",
            isFeatured = true, isOpen = true
        ),
        Place(
            id = "hotel_babylon",
            name = "Babylon Rotana Karbala",
            nameArabic = "فندق بابيلون روتانا كربلاء",
            category = PlaceCategory.HOTEL,
            description = "فندق فاخر بإطلالة على الحرمين، خدمات عالمية المستوى",
            latitude = 32.6155, longitude = 44.0300,
            address = "شارع الإمام علي، كربلاء",
            phone = "07801234568",
            rating = 4.7f, reviewCount = 2800,
            priceRange = "200,000 - 600,000 د.ع",
            openingHours = "24 ساعة",
            isFeatured = true, isOpen = true
        ),
        Place(
            id = "hotel_raad",
            name = "Raad Hotel",
            nameArabic = "فندق الرعد",
            category = PlaceCategory.HOTEL,
            description = "فندق اقتصادي نظيف قريب من الحرمين",
            latitude = 32.6130, longitude = 44.0315,
            address = "كربلاء المقدسة",
            phone = "07701234567",
            rating = 3.8f, reviewCount = 890,
            priceRange = "50,000 - 120,000 د.ع",
            openingHours = "24 ساعة",
            isOpen = true
        ),

        // ── RESTAURANTS ─────────────────────────────────────
        Place(
            id = "rest_mazaj",
            name = "Mazaj Restaurant",
            nameArabic = "مطعم مزاج كربلاء",
            category = PlaceCategory.RESTAURANT,
            description = "أفضل مطاعم كربلاء، مشاوي عراقية أصيلة وأسماك طازجة",
            latitude = 32.6080, longitude = 44.0200,
            address = "شارع أبو العباس، كربلاء",
            phone = "07901234567",
            rating = 4.6f, reviewCount = 1850,
            priceRange = "10,000 - 50,000 د.ع",
            openingHours = "12:00 - 24:00",
            isFeatured = true, isOpen = true
        ),
        Place(
            id = "rest_tigris",
            name = "Tigris Fish Restaurant",
            nameArabic = "مطعم دجلة للأسماك",
            category = PlaceCategory.RESTAURANT,
            description = "تخصص في الأسماك العراقية المشوية والمسگوف الأصيل",
            latitude = 32.6090, longitude = 44.0210,
            address = "كورنيش الفرات، كربلاء",
            phone = "07801234569",
            rating = 4.4f, reviewCount = 1200,
            priceRange = "15,000 - 60,000 د.ع",
            openingHours = "11:00 - 23:00",
            isOpen = true
        ),
        Place(
            id = "rest_hussaini",
            name = "Al-Hussaini Restaurant",
            nameArabic = "مطعم الحسيني",
            category = PlaceCategory.RESTAURANT,
            description = "مطعم شعبي للزوار، كباب وتكة وقيمر",
            latitude = 32.6165, longitude = 44.0340,
            address = "شارع الإمام الحسين، كربلاء",
            phone = "07701234568",
            rating = 4.2f, reviewCount = 980,
            priceRange = "5,000 - 25,000 د.ع",
            openingHours = "07:00 - 22:00",
            isOpen = true
        ),

        // ── HOSPITALS ────────────────────────────────────────
        Place(
            id = "hosp_imam",
            name = "Imam Hussein Teaching Hospital",
            nameArabic = "مستشفى الإمام الحسين التعليمي",
            category = PlaceCategory.HOSPITAL,
            description = "أكبر مستشفيات كربلاء، طوارئ على مدار الساعة",
            latitude = 32.6020, longitude = 44.0180,
            address = "شارع الطبيب، كربلاء المقدسة",
            phone = "07801234570",
            rating = 3.9f, reviewCount = 450,
            openingHours = "24 ساعة",
            isFeatured = true, isOpen = true
        ),
        Place(
            id = "hosp_karbala",
            name = "Karbala General Hospital",
            nameArabic = "مستشفى كربلاء العام",
            category = PlaceCategory.HOSPITAL,
            description = "مستشفى حكومي، تخصصات متعددة",
            latitude = 32.6050, longitude = 44.0250,
            address = "كربلاء المقدسة",
            phone = "036-321456",
            rating = 3.5f, reviewCount = 320,
            openingHours = "24 ساعة",
            isOpen = true
        ),

        // ── TOURISM ──────────────────────────────────────────
        Place(
            id = "tourism_museum",
            name = "Karbala Museum",
            nameArabic = "متحف كربلاء",
            category = PlaceCategory.TOURISM,
            description = "متحف يوثّق تاريخ كربلاء وآثارها الإسلامية والبابلية",
            latitude = 32.6100, longitude = 44.0290,
            address = "وسط المدينة، كربلاء",
            rating = 4.1f, reviewCount = 560,
            openingHours = "09:00 - 17:00",
            isFeatured = true, isOpen = true
        ),
        Place(
            id = "tourism_lake",
            name = "Karbala Lake (Bahr Najaf)",
            nameArabic = "بحر النجف - بحيرة كربلاء",
            category = PlaceCategory.TOURISM,
            description = "منطقة طبيعية جميلة على أطراف كربلاء للتنزه والاسترخاء",
            latitude = 32.5500, longitude = 43.9800,
            address = "خارج كربلاء باتجاه النجف",
            rating = 4.3f, reviewCount = 780,
            openingHours = "24 ساعة",
            isOpen = true
        ),

        // ── MARKETS ─────────────────────────────────────────
        Place(
            id = "market_old",
            name = "Old Bazaar of Karbala",
            nameArabic = "سوق كربلاء القديم",
            category = PlaceCategory.MARKET,
            description = "السوق التاريخي التقليدي، عبايات وتحف وهدايا دينية",
            latitude = 32.6150, longitude = 44.0310,
            address = "بجانب الحرمين، كربلاء",
            rating = 4.0f, reviewCount = 1100,
            openingHours = "08:00 - 22:00",
            isFeatured = true, isOpen = true
        ),

        // ── BANKS ───────────────────────────────────────────
        Place(
            id = "bank_rasheed",
            name = "Al-Rasheed Bank - Karbala Branch",
            nameArabic = "مصرف الرشيد - فرع كربلاء",
            category = PlaceCategory.BANK,
            description = "خدمات مصرفية كاملة، صرافة، تحويلات",
            latitude = 32.6120, longitude = 44.0270,
            address = "شارع بابل، كربلاء",
            phone = "036-312345",
            rating = 3.7f, reviewCount = 230,
            openingHours = "08:30 - 14:30",
            isOpen = true
        ),

        // ── GOVERNMENT ──────────────────────────────────────
        Place(
            id = "gov_province",
            name = "Karbala Provincial Council",
            nameArabic = "مجلس محافظة كربلاء",
            category = PlaceCategory.GOVERNMENT,
            description = "مقر مجلس المحافظة وديوان المحافظة",
            latitude = 32.6070, longitude = 44.0230,
            address = "شارع المحافظة، كربلاء",
            phone = "036-330000",
            rating = 3.2f, reviewCount = 150,
            openingHours = "08:00 - 14:00",
            isOpen = true
        )
    )
}
