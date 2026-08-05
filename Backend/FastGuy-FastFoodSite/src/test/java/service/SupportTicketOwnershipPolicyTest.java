package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import entity.SupportTicket;
import entity.User;

class SupportTicketOwnershipPolicyTest {
    @Test
    void openTicketCanOnlyBeClaimedByCaller() {
        SupportTicket ticket = ticket("OPEN", null);

        assertTrue(SupportTicketService.canUpdate(ticket, 10, "PROCESSING"));
        assertFalse(SupportTicketService.canUpdate(ticket, 10, "RESOLVED"));
    }

    @Test
    void assignedTicketCanOnlyBeUpdatedByAssignee() {
        SupportTicket ticket = ticket("PROCESSING", 10);

        assertTrue(SupportTicketService.canUpdate(ticket, 10, "PROCESSING"));
        assertTrue(SupportTicketService.canUpdate(ticket, 10, "RESOLVED"));
        assertFalse(SupportTicketService.canUpdate(ticket, 11, "PROCESSING"));
        assertFalse(SupportTicketService.canUpdate(ticket, 11, "RESOLVED"));
    }

    @Test
    void ownershipCheckRunsInsideLockedTransactionAndConflictMapsTo409() throws IOException {
        String service = Files.readString(Path.of("src/main/java/service/SupportTicketService.java"));
        String servlet = Files.readString(Path.of("src/main/java/servlet/StaffSupportTicketServlet.java"));

        assertTrue(service.indexOf("PESSIMISTIC_WRITE") < service.indexOf("canUpdate(ticket, staffId, status)"));
        assertTrue(servlet.contains("SupportTicketService.OwnershipConflictException"));
        assertTrue(servlet.contains("ApiResponse.error(resp, e.getMessage(), 409)"));
    }

    private SupportTicket ticket(String status, Integer staffId) {
        SupportTicket ticket = new SupportTicket();
        ticket.setStatus(status);
        if (staffId != null) {
            User staff = new User();
            staff.setUserId(staffId);
            ticket.setStaff(staff);
        }
        return ticket;
    }
}
