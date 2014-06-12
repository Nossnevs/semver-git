package com.cinnober.gradle.semver.git

import org.gradle.api.Project
import org.gradle.api.Plugin

class SemverGitPlugin implements Plugin<Project> {

    def static String getGitVersion(String nextVersion, String snapshotSuffix) {
        def proc = "git describe --exact-match".execute();
        proc.waitFor();
        if (proc.exitValue() == 0) {
            return checkVersion(proc.text.trim());
        }
        proc = "git describe".execute();
        proc.waitFor();
        if (proc.exitValue() == 0) {
            def describe = proc.text.trim()
            def version = (describe =~ /-[0-9]+-g[0-9a-f]+$/).replaceFirst("")
            def suffixMatcher = (describe =~ /-([0-9]+)-g([0-9a-f]+)$/)
            def count = suffixMatcher[0][1];
            def sha = suffixMatcher[0][2];
            def suffix = snapshotSuffix;
            suffix = suffix.replaceAll("<count>", count);
            suffix = suffix.replaceAll("<sha>", sha);
            return getNextVersion(version, nextVersion, suffix);
        }
        return getNextVersion("0.0.0", nextVersion, "SNAPSHOT")
    }

    def static checkVersion(String version) {
        parseVersion(version);
        return version;
    }

    def static parseVersion(String version) {
        def pattern = /([0-9]+)\.([0-9]+)\.([0-9]+)(-([a-zA-Z0-9.-]+))?/
        def matcher = version =~ pattern
        def arr = matcher.collect { it }[0]
        if (arr == null) {
            throw new IllegalArgumentException("Not a valid version: '" + version + "'")
        }
        return [arr[1].toInteger(), arr[2].toInteger(), arr[3].toInteger(), arr[5]]
    }

    def static formatVersion(version) {
        return "" + version[0] + "." + version[1] + "." + version[2] + (version[3] != null ? "-" + version[3] : "");
    }

    def static String getNextVersion(String version, String nextVersion, String snapshotSuffix) {
        def v
        switch (nextVersion) {
            case "major":
                v = parseVersion(version)
                if (v[3] == null) {
                    v[0] += 1
                    v[1] = 0
                    v[2] = 0
                }
                v[3] = snapshotSuffix
                return formatVersion(v)
            case "minor":
                v = parseVersion(version)
                if (v[3] == null) {
                    v[1] += 1
                    v[2] = 0
                }
                v[3] = snapshotSuffix
                return formatVersion(v);
            case "patch":
                v = parseVersion(version)
                if (v[3] == null) {
                    v[2] += 1
                }
                v[3] = snapshotSuffix
                return formatVersion(v);
            default:
                return checkVersion(nextVersion);
        }
    }

    void apply(Project project) {
        def nextVersion = "minor"
        def snapshotSuffix = "SNAPSHOT"
        if (project.ext.properties.containsKey("nextVersion")) {
            nextVersion = project.ext.nextVersion
        }
        if (project.ext.properties.containsKey("snapshotSuffix")) {
            snapshotSuffix = project.ext.snapshotSuffix
        }
        project.version = getGitVersion(nextVersion, snapshotSuffix)
        project.task('showVersion') {
            group = 'Help'
            description = 'Show the project version'
        }
        project.tasks.showVersion << {
            println "Version: " + project.version
        }
    }
}

//println "parse (1.2.3) = " + SemverGitPlugin2.parseVersion("1.2.3")
//println "format [1,2,3,null]=" + SemverGitPlugin2.formatVersion(SemverGitPlugin2.parseVersion("1.2.3"))
//println "format [1,2,3,null]=" + SemverGitPlugin2.formatVersion([1,2,3,null])
//println "format [1,2,3,rc1]=" + SemverGitPlugin2.formatVersion([1,2,3,"rc1"])
//println "parse (1.2.3-rc2) = " + SemverGitPlugin2.parseVersion("1.2.3-rc2")
//println "nextVer(1.2.3) = " + SemverGitPlugin2.getNextVersion("1.2.3", "major")
//println "nextVer(1.2.3) = " + SemverGitPlugin2.getNextVersion("1.2.3", "minor")
//println "nextVer(1.2.3) = " + SemverGitPlugin2.getNextVersion("1.2.3", "patch")
//println "nextVer(1.2.3-rc2) = " + SemverGitPlugin2.getNextVersion("1.2.3-rc2", "major")
//println "nextVer(1.2.3-rc2) = " + SemverGitPlugin2.getNextVersion("1.2.3-rc2", "minor")
//println "nextVer(1.2.3-rc2) = " + SemverGitPlugin2.getNextVersion("1.2.3-rc2", "patch")
//println "nextVer(1.2.3-rc2) = " + SemverGitPlugin2.getNextVersion("1.2.3-rc2", "4.5.6")
//println "nextVer(1.2.3) = " + SemverGitPlugin2.getNextVersion("1.2.3", "minor", "SNAPSHOT")
//println "nextVer(1.2.3) = " + SemverGitPlugin2.getNextVersion("1.2.3", "minor", "git.5.sha.g4234234")
