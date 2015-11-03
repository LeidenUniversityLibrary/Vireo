package org.tdl.vireo.model;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.web.WebAppConfiguration;
import org.tdl.vireo.Application;
import org.tdl.vireo.runner.OrderedRunner;

@WebAppConfiguration
@RunWith(OrderedRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
public class AttachmentTypeTest extends AbstractEntityTest {

    @Before
    public void setUp() {
        assertEquals("AttachmentType repo was not empty!", 0, attachmentTypeRepo.count());
    }

    @Override
    public void testCreate() {
        AttachmentType testAttachmentType = attachmentTypeRepo.create(TEST_ATTACHMENT_TYPE_NAME);
        assertEquals("Embargo Repo did not save the embargo!", 1, attachmentTypeRepo.count());
        assertEquals("Embargo Repo did not save the correct embargo name!", TEST_ATTACHMENT_TYPE_NAME, testAttachmentType.getName());
     }
    
    @Override
    public void testDuplication() {
        attachmentTypeRepo.create(TEST_ATTACHMENT_TYPE_NAME);
        assertEquals("The repository didn't persist attachment type!", 1, attachmentTypeRepo.count());
        try {
            attachmentTypeRepo.create(TEST_ATTACHMENT_TYPE_NAME);
        }
        catch (DataIntegrityViolationException e) { /* SUCCESS */ }
        assertEquals("The repository didn't persist attachment type!", 1, attachmentTypeRepo.count());
    }

    @Override
    public void testDelete() {
        AttachmentType testAttachmentType = attachmentTypeRepo.create(TEST_ATTACHMENT_TYPE_NAME);
        attachmentTypeRepo.delete(testAttachmentType);
        assertEquals("AttachmentType did not delete!", 0, attachmentTypeRepo.count());
    }

    @Override
    public void testCascade() {
        // nothing to cascade
    }

    @After
    public void cleanUp() {
        attachmentTypeRepo.deleteAll();
    }
    
}