package org.tdl.vireo.controller;

import static edu.tamu.weaver.response.ApiStatus.SUCCESS;
import static edu.tamu.weaver.validation.model.BusinessValidationType.CREATE;
import static edu.tamu.weaver.validation.model.BusinessValidationType.DELETE;
import static edu.tamu.weaver.validation.model.BusinessValidationType.EXISTS;
import static edu.tamu.weaver.validation.model.BusinessValidationType.NONEXISTS;
import static edu.tamu.weaver.validation.model.BusinessValidationType.UPDATE;
import static edu.tamu.weaver.validation.model.MethodValidationType.REORDER;
import static edu.tamu.weaver.validation.model.MethodValidationType.SORT;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.tdl.vireo.model.EmailTemplate;
import org.tdl.vireo.model.repo.EmailTemplateRepo;

import edu.tamu.framework.aspect.annotation.ApiMapping;
import edu.tamu.framework.aspect.annotation.ApiVariable;
import edu.tamu.framework.aspect.annotation.Auth;
import edu.tamu.weaver.response.ApiResponse;
import edu.tamu.weaver.validation.aspect.annotation.WeaverValidatedModel;
import edu.tamu.weaver.validation.aspect.annotation.WeaverValidation;

@Controller
@ApiMapping("/settings/email-template")
public class EmailTemplateController {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EmailTemplateRepo emailTemplateRepo;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @ApiMapping("/all")
    @Auth(role = "MANAGER")
    public ApiResponse allEmailTemplates() {
        return new ApiResponse(SUCCESS, emailTemplateRepo.findAllByOrderByPositionAsc());
    }

    @Auth(role = "MANAGER")
    @ApiMapping(value = "/create", method = POST)
    @WeaverValidation(business = { @WeaverValidation.Business(value = CREATE), @WeaverValidation.Business(value = EXISTS) })
    public ApiResponse createEmailTemplate(@WeaverValidatedModel EmailTemplate emailTemplate) {
        logger.info("Creating email template with name " + emailTemplate.getName());
        emailTemplate = emailTemplateRepo.create(emailTemplate.getName(), emailTemplate.getSubject(), emailTemplate.getMessage());
        simpMessagingTemplate.convertAndSend("/channel/settings/email-template", new ApiResponse(SUCCESS, emailTemplateRepo.findAllByOrderByPositionAsc()));
        return new ApiResponse(SUCCESS, emailTemplate);
    }

    @Auth(role = "MANAGER")
    @ApiMapping(value = "/update", method = POST)
    @WeaverValidation(business = { @WeaverValidation.Business(value = UPDATE), @WeaverValidation.Business(value = NONEXISTS) })
    public ApiResponse updateEmailTemplate(@WeaverValidatedModel EmailTemplate emailTemplate) {
        logger.info("Updating email template with name " + emailTemplate.getName());
        if (emailTemplate.getSystemRequired()) {
            emailTemplate = emailTemplateRepo.create(emailTemplate.getName(), emailTemplate.getSubject(), emailTemplate.getMessage());
        } else {
            emailTemplate = emailTemplateRepo.save(emailTemplate);
        }
        simpMessagingTemplate.convertAndSend("/channel/settings/email-template", new ApiResponse(SUCCESS, emailTemplateRepo.findAllByOrderByPositionAsc()));
        return new ApiResponse(SUCCESS, emailTemplate);
    }

    @Auth(role = "MANAGER")
    @ApiMapping(value = "/remove", method = POST)
    @WeaverValidation(business = { @WeaverValidation.Business(value = DELETE, path = { "systemRequired" }, restrict = "true"), @WeaverValidation.Business(value = NONEXISTS) })
    public ApiResponse removeEmailTemplate(@WeaverValidatedModel EmailTemplate emailTemplate) {
        logger.info("Removing email template with name " + emailTemplate.getName());
        emailTemplateRepo.remove(emailTemplate);
        simpMessagingTemplate.convertAndSend("/channel/settings/email-template", new ApiResponse(SUCCESS, emailTemplateRepo.findAllByOrderByPositionAsc()));
        return new ApiResponse(SUCCESS);
    }

    @ApiMapping("/reorder/{src}/{dest}")
    @Auth(role = "MANAGER")
    @WeaverValidation(method = { @WeaverValidation.Method(value = REORDER, model = EmailTemplate.class, params = { "0", "1" }) })
    public ApiResponse reorderEmailTemplates(@ApiVariable Long src, @ApiVariable Long dest) {
        logger.info("Reordering document types");
        emailTemplateRepo.reorder(src, dest);
        simpMessagingTemplate.convertAndSend("/channel/settings/email-template", new ApiResponse(SUCCESS, emailTemplateRepo.findAllByOrderByPositionAsc()));
        return new ApiResponse(SUCCESS);
    }

    @ApiMapping("/sort/{column}")
    @Auth(role = "MANAGER")
    @WeaverValidation(method = { @WeaverValidation.Method(value = SORT, model = EmailTemplate.class, params = { "0" }) })
    public ApiResponse sortEmailTemplates(@ApiVariable String column) {
        logger.info("Sorting email templates by " + column);
        emailTemplateRepo.sort(column);
        simpMessagingTemplate.convertAndSend("/channel/settings/email-template", new ApiResponse(SUCCESS, emailTemplateRepo.findAllByOrderByPositionAsc()));
        return new ApiResponse(SUCCESS);
    }

}
