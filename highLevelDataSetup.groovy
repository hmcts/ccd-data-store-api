#!groovy
def call(params) {
    def targetEnvironment = params.targetEnvironment
    def product = params.product

    stageWithAgent("High Level Data Setup - ${targetEnvironment}", product) {
        echo "I am a new stage"
        echo "Target Env " + targetEnvironment
        sh './gradlew highLevelDataSetup --args=${targetEnvironment}'
    }
}
