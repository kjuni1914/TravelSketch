dependencies {
    implementation("com.airbnb.android:lottie-compose:6.3.0")
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.travelsketch"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.travelsketch"
        minSdk = 33
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.compose.material:material-icons-extended:1.5.1") // 준희 // 비밀번호 보이기|가리기 버튼
    implementation("com.google.android.gms:play-services-auth:20.7.0") // 준희 // 구글 로그인 API
    implementation("androidx.compose.material:material-icons-extended:1.5.1") // 준희 // 비밀번호 보이기|가리기 버튼
    implementation("com.google.android.gms:play-services-auth:20.7.0") // 준희 // 구글 로그인 API
    implementation("com.kakao.sdk:v2-all:2.20.6") // 전체 모듈 설치, 2.11.0 버전부터 지원
    implementation("com.kakao.sdk:v2-user:2.20.6") // 카카오 로그인 API 모듈
    implementation("com.kakao.sdk:v2-share:2.20.6") // 카카오톡 공유 API 모듈
    // implementation("com.kakao.sdk:v2-talk:2.20.6") 카카오톡 채널, 카카오톡 소셜, 카카오톡 메시지 API 모듈
    // implementation("com.kakao.sdk:v2-friend:2.20.6") 피커 API 모듈
    // implementation("com.kakao.sdk:v2-navi:2.20.6") 카카오내비 API 모듈
    implementation("com.kakao.sdk:v2-cert:2.20.6") // 카카오톡 인증 서비스 API 모듈
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-database")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.2")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.google.maps)
    implementation(libs.maps.compose)
    implementation(libs.androidx.material3)
    implementation(libs.google.maps.utils)
    implementation(libs.google.maps.compose)
    implementation ("androidx.fragment:fragment-ktx:1.6.1")
    implementation ("io.coil-kt:coil-compose:2.0.0") // image dependency
    implementation("com.squareup.retrofit2:retrofit:2.9.0") // geocoding api
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // geocoding api
    implementation ("com.squareup.okhttp3:okhttp:4.11.0") // geocoding api
    implementation ("com.google.code.gson:gson:2.10.1") // geocoding api
//    implementation ("com.google.android.exoplayer:exoplayer:2.19.0") // media video 재생
    implementation(libs.play.services.location) // 최신 버전 확인 후 업데이트
    implementation ("com.google.firebase:firebase-messaging:23.1.2") // fcm push 알림
    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.0") // view model lifecycle


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
