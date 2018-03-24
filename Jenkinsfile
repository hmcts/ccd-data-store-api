#!groovy
@Library("Reform")
import uk.gov.hmcts.Ansible
import uk.gov.hmcts.Packager
import uk.gov.hmcts.RPMTagger

packager = new Packager(this, 'ccdata');
ansible = new Ansible(this, 'ccdata');
server = Artifactory.server 'artifactory.reform'
buildInfo = Artifactory.newBuildInfo()

properties(
    [[$class: 'GithubProjectProperty', displayName: 'Case Data Store API', projectUrlStr: 'https://github.com/hmcts/ccd-data-store-api'],
     pipelineTriggers([[$class: 'GitHubPushTrigger']]),
    [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '7', numToKeepStr: '10']]
    ],
)

milestone()
lock(resource: "case-data-store-app-${env.BRANCH_NAME}", inversePrecedence: true) {
    node {
        try {
            stage('Checkout') {
                deleteDir()
                checkout scm
            }

            timeout(15) {
                stage('Build') {
                    sh "./gradlew clean build sonar -i -Dsonar.projectName=\"CCD :: Case Data Store API\" " +
                        " -Dsonar.projectKey=CCD_Case_Data_Store_API" +
                        " -Dsonar.host.url=https://sonar.reform.hmcts.net/ "
                }
            }

            timeout (25) {
                onMaster {
                    publishAndDeploy('master', 'test')
                }

                onDevelop {
                    publishAndDeploy('develop', 'dev')
                }
            }

            milestone()
        } catch (err) {
            notifyBuildFailure channel: '#ccd-notifications'
            throw err
        } finally {
            junit 'build/test-results/junit-platform/*.xml'
        }
    }
}

def publishAndDeploy(branch, env) {
    def rpmVersion
    def version
    // Temporary port offset avoiding collision till Dev and Test environments are fully separated by DevOps
    def backendPort = (env == 'test') ? '4481' : '4451'

    stage('Publish JAR') {
        server.publishBuildInfo buildInfo
    }

    stage('Publish RPM') {
        rpmVersion = packager.javaRPM(branch, 'case-data-store-api',
            'build/libs/core-case-data-$(./gradlew -q projectVersion)-all.jar',
            'springboot', 'src/main/resources/application.properties')
        packager.publishJavaRPM('case-data-store-api')
    }

    stage('Package (Docker)') {
        dataStoreVersion = dockerImage imageName: 'ccd/ccd-data-store-api', tags: [branch]
        dataStoreDatabaseVersion = dockerImage imageName: 'ccd/ccd-data-store-database', context: 'docker/database', tags: [branch]
    }

    def rpmTagger = new RPMTagger(
        this,
        'case-data-store-api',
        packager.rpmName('case-data-store-api', rpmVersion),
        'ccdata-local'
    )

    stage('Deploy: ' + env) {
        version = "{ccd_datamgmt_api_version: ${rpmVersion}}"
        ansible.runDeployPlaybook(version, env, branch)
        rpmTagger.tagDeploymentSuccessfulOn(env)
    }

    stage('Smoke Tests: ' + env) {
        sh "curl -vf https://case-data-app." + env + ".ccd.reform.hmcts.net:" + backendPort + "/status/health"
        rpmTagger.tagTestingPassedOn(env)
    }
}
