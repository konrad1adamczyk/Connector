plugins {
    `java-library`
}

dependencies {
    api(project(":spi:data-plane:data-plane-spi"))

    implementation(project(":core:common:util"))
    implementation(project(":extensions:control-plane:transfer:transfer-data-plane"))
    implementation(project(":core:data-plane:data-plane-util"))
    implementation("org.eclipse.milo:sdk-client:0.6.0") // Or whichever version you need

    testImplementation(project(":core:common:junit"))
    testImplementation("org.mockito:mockito-core:3.0.0") // Or whichever version you need
    testImplementation("org.awaitility:awaitility:4.0.1") // Or whichever version you need
}

