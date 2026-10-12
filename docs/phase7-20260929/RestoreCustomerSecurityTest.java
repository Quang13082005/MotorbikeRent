package com.motorrent.motorbikerental;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RestoreCustomerSecurityTest {
    public static void main(String[] args) throws Exception {
        Path beforeFile = Paths.get("docs/phase5-20260929/CustomerSecurityIntegrationTests.before.java");
        Path targetFile = Paths.get("src/test/java/com/motorrent/motorbikerental/CustomerSecurityIntegrationTests.java");

        String beforeContent = Files.readString(beforeFile, StandardCharsets.UTF_8);

        // Phase 5 tests to insert right after setup()
        String phase5Tests = "\n  @Test void customerCannotInitiateOrCancelVehicleChangeEvenForOwnRental() throws Exception {\n" +
                "   var changes=context.getBean(RentalVehicleChangeRepository.class);\n" +
                "   long before=changes.count();\n" +
                "   String route=\"/api/operations/orders/\"+order.getId();\n" +
                "   String body=\"{\\\"motorbikeId\\\":\"+bike.getId()+\",\\\"reason\\\":\\\"Customer attempted change\\\",\\\"staffId\\\":\\\"\"+staff.getId()+\"\\\"}\";\n" +
                "   for(var state:java.util.List.of(RentalStatus.PENDING,RentalStatus.HANDED_OVER)) {\n" +
                "    order.setStatus(state);orders.saveAndFlush(order);\n" +
                "    mvc.perform(get(route+\"/replacement-options\").with(user(\"alice\").roles(\"CUSTOMER\"))).andExpect(status().isForbidden());\n" +
                "    for(String suffix:java.util.List.of(\"/vehicle-changes/quote\",\"/vehicle-changes\"))\n" +
                "     mvc.perform(post(route+suffix).with(user(\"alice\").roles(\"CUSTOMER\")).with(csrf()).contentType(\"application/json\").content(body)).andExpect(status().isForbidden());\n" +
                "    mvc.perform(delete(\"/api/operations/vehicle-changes/123\").with(user(\"alice\").roles(\"CUSTOMER\")).with(csrf())).andExpect(status().isForbidden());\n" +
                "    assertThat(orders.findById(order.getId()).orElseThrow().getMotorbike().getId()).isEqualTo(bike.getId());\n" +
                "    assertThat(order.getStatus()).isEqualTo(state);\n" +
                "   }\n" +
                "   assertThat(changes.count()).isEqualTo(before);\n" +
                "   assertThat(payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId())).isEmpty();\n" +
                "  }\n" +
                "  @Test void vehicleChangeServiceUsesStoredStaffRoleNotSubmittedActorOrForgedAuthority() throws Exception {\n" +
                "   String route=\"/api/operations/orders/\"+order.getId();\n" +
                "   // Pass the route-level role check, then require the real persisted account role.\n" +
                "   for(String suffix:java.util.List.of(\"/vehicle-changes/quote\",\"/vehicle-changes\"))\n" +
                "    mvc.perform(post(route+suffix).with(user(\"alice\").roles(\"STAFF\")).with(csrf()).contentType(\"application/json\")\n" +
                "     .content(\"{\\\"motorbikeId\\\":\"+bike.getId()+\",\\\"reason\\\":\\\"Forged actor\\\",\\\"staffId\\\":\\\"\"+staff.getId()+\"\\\"}\"))\n" +
                "     .andExpect(status().isForbidden());\n" +
                "   mvc.perform(post(route+\"/vehicle-changes\").with(user(\"operator\").roles(\"STAFF\")).contentType(\"application/json\").content(\"{}\"))\n" +
                "    .andExpect(status().isForbidden());\n" +
                "  }\n";

        String setupMarker = "User makeUser(String name,UserRole role){return users.save(User.builder().username(name).email(name+\"@example.test\").password(\"unused\").firstName(name).lastName(\"Test\").role(role).build());}";
        if (!beforeContent.contains(setupMarker)) {
            throw new IllegalStateException("setupMarker not found in beforeContent!");
        }

        String merged = beforeContent.replace(setupMarker, setupMarker + phase5Tests);

        // Update hasSize(4) to hasSize(5) in complete lifecycle test because Phase 7 complete creates ASSET_DEPOSIT_REFUND
        String oldAssertion = "assertThat(payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId())).hasSize(4);";
        String newAssertion = "assertThat(payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId())).hasSize(5);";
        if (merged.contains(oldAssertion)) {
            merged = merged.replace(oldAssertion, newAssertion);
        } else {
            System.out.println("Note: oldAssertion hasSize(4) not found or already 5");
        }

        Files.writeString(targetFile, merged, StandardCharsets.UTF_8);
        System.out.println("Successfully restored CustomerSecurityIntegrationTests.java with clean UTF-8. Length: " + Files.size(targetFile));
    }
}
