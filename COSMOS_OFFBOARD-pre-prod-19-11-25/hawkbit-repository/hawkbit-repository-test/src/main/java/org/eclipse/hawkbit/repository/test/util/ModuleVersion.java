package org.eclipse.hawkbit.repository.test.util;

import java.util.List;

public class ModuleVersion {
    List<Long> modules;
    List<Integer> versionList;

    public List<Long> getModules() {
        return modules;
    }

    public void setModules(List<Long> modules) {
        this.modules = modules;
    }

    public List<Integer> getVersionList() {
        return versionList;
    }

    public void setVersionList(List<Integer> versionList) {
        this.versionList = versionList;
    }
}
