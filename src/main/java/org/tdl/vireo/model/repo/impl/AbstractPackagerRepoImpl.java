package org.tdl.vireo.model.repo.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.tdl.vireo.model.formatter.AbstractFormatter;
import org.tdl.vireo.model.packager.AbstractPackager;
import org.tdl.vireo.model.packager.DSpaceMetsPackager;
import org.tdl.vireo.model.packager.DSpaceSimplePackager;
import org.tdl.vireo.model.packager.ExcelPackager;
import org.tdl.vireo.model.packager.Marc21Packager;
import org.tdl.vireo.model.packager.MarcXML21Packager;
import org.tdl.vireo.model.packager.Packager;
import org.tdl.vireo.model.packager.ProQuestUmiPackager;
import org.tdl.vireo.model.repo.AbstractPackagerRepo;
import org.tdl.vireo.model.repo.custom.AbstractPackagerRepoCustom;

import edu.tamu.weaver.data.model.repo.impl.AbstractWeaverRepoImpl;

public class AbstractPackagerRepoImpl extends AbstractWeaverRepoImpl<AbstractPackager<?>, AbstractPackagerRepo> implements AbstractPackagerRepoCustom {

    @Autowired
    private AbstractPackagerRepo abstractPackagerRepo;

    @Override
    public Packager<?> createDSpaceMetsPackager(String name, AbstractFormatter formatter) {
        return abstractPackagerRepo.save(new DSpaceMetsPackager(name, formatter));
    }

    @Override
    public Packager<?> createDSpaceSimplePackager(String name, AbstractFormatter formatter) {
        return abstractPackagerRepo.save(new DSpaceSimplePackager(name, formatter));
    }

    @Override
    public Packager<?> createProQuestUmiPackager(String name, AbstractFormatter formatter) {
        return abstractPackagerRepo.save(new ProQuestUmiPackager(name, formatter));
    }

    @Override
    public Packager<?> createExcelPackager(String name, AbstractFormatter formatter) {
        return abstractPackagerRepo.save(new ExcelPackager(name, formatter));
    }

    @Override
    public Packager<?> createMARC21XMLPackager(String name, AbstractFormatter formatter) {
        return abstractPackagerRepo.save(new MarcXML21Packager(name, formatter));
    }

    @Override
    public Packager<?> createMARC21Packager(String name, AbstractFormatter formatter) {
        return abstractPackagerRepo.save(new Marc21Packager(name, formatter));
    }

    @Override
    protected String getChannel() {
        return "/channel/abstract-packager";
    }

}
