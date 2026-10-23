package com.returnloop.bootstrap;

import com.returnloop.catalog.Product;
import com.returnloop.catalog.ProductCategory;
import com.returnloop.catalog.ProductRepository;
import com.returnloop.common.config.ReturnLoopProperties;
import com.returnloop.identity.user.UserAccount;
import com.returnloop.identity.user.UserAccountRepository;
import com.returnloop.logistics.Driver;
import com.returnloop.logistics.DriverRepository;
import com.returnloop.logistics.Location;
import com.returnloop.logistics.LocationRepository;
import com.returnloop.logistics.LocationType;
import com.returnloop.logistics.Vehicle;
import com.returnloop.logistics.VehicleRepository;
import com.returnloop.organization.Organization;
import com.returnloop.organization.OrganizationRepository;
import com.returnloop.partner.Partner;
import com.returnloop.partner.PartnerRepository;
import com.returnloop.partner.PartnerType;
import com.returnloop.security.Role;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds a synthetic demo organisation on first start.
 *
 * <p>The data is clearly labelled as demo data and exists so the platform can
 * be explored immediately after a local install. Partner performance metrics
 * are left at their defaults; they are derived from real completed jobs
 * rather than seeded, so nothing in the UI is a fabricated statistic.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "returnloop.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final OrganizationRepository organizationRepository;
    private final UserAccountRepository userRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final PartnerRepository partnerRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReturnLoopProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (organizationRepository.count() > 0) {
            log.info("Seed skipped: the database already contains organisations");
            return;
        }
        log.info("Seeding demo organisation and reference data");

        Organization organization = new Organization();
        organization.setName("Northwind Retail Group");
        organization.setSlug("northwind-retail");
        organization.setContactEmail("ops@northwind-retail.example");
        organization.setCountry("US");
        organization.setDefaultCurrency("USD");
        organization.setHoldingThresholdDays(30);
        Organization savedOrganization = organizationRepository.save(organization);
        java.util.UUID organizationId = savedOrganization.getId();

        String demoPassword = properties.getSeed().getDemoPassword();
        seedUsers(organizationId, demoPassword);
        seedProducts(organizationId);
        seedLocations(organizationId);
        seedFleet(organizationId);
        seedPartners(organizationId);

        log.info("Demo data seeded. Sign in as ops@northwind-retail.example with the configured demo password.");
    }

    private void seedUsers(java.util.UUID organizationId, String demoPassword) {
        userRepository.save(user(organizationId, "ops@northwind-retail.example", "Amara Okafor", "demoPassword",
                Role.ADMIN, "Head of Reverse Operations", demoPassword));
        userRepository.save(user(organizationId, "warehouse@northwind-retail.example", "Tomas Berg",
                "Northwind Warehouse", Role.WAREHOUSE_INSPECTOR, "Warehouse Inspector", demoPassword));
        userRepository.save(user(organizationId, "logistics@northwind-retail.example", "Priya Raman",
                "Northwind Logistics", Role.LOGISTICS_PLANNER, "Logistics Planner", demoPassword));
        userRepository.save(user(organizationId, "analyst@northwind-retail.example", "Jonas Lindqvist",
                "Northwind Analytics", Role.ANALYST, "Supply Chain Analyst", demoPassword));
    }

    private UserAccount user(java.util.UUID organizationId, String email, String fullName, String ignored,
                             Role role, String jobTitle, String password) {
        UserAccount user = new UserAccount();
        user.setOrganizationId(organizationId);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);
        user.setJobTitle(jobTitle);
        user.setActive(true);
        user.setPasswordHash(passwordEncoder.encode(password));
        return user;
    }

    private void seedProducts(java.util.UUID organizationId) {
        product(organizationId, "NWR-PH-0142", "Aurora X1 Smartphone", "Aurora", "X1", ProductCategory.MOBILE_PHONES,
                "128 GB, graphite", new BigDecimal("0.42"), new BigDecimal("899.00"), new BigDecimal("612.00"));
        product(organizationId, "NWR-LP-0311", "Meridian 14 Ultrabook", "Meridian", "14", ProductCategory.LAPTOPS,
                "16 GB RAM, 512 GB SSD", new BigDecimal("1.65"), new BigDecimal("1699.00"), new BigDecimal("1040.00"));
        product(organizationId, "NWR-TV-0088", "Halcyon 55\" OLED", "Halcyon", "55OLED", ProductCategory.ELECTRONICS,
                "4K OLED television", new BigDecimal("19.80"), new BigDecimal("2199.00"), new BigDecimal("1310.00"));
        product(organizationId, "NWR-BL-0504", "Kettle 1.7L Steam", "Kettleworks", "S170", ProductCategory.APPLIANCES,
                "Steam kettle, stainless steel", new BigDecimal("1.10"), new BigDecimal("129.00"), new BigDecimal("74.00"));
        product(organizationId, "NWR-AU-0221", "Aria Studio Headphones", "Aria", "Studio", ProductCategory.ELECTRONICS,
                "Over-ear, active noise cancelling", new BigDecimal("0.38"), new BigDecimal("349.00"), new BigDecimal("198.00"));
        product(organizationId, "NWR-CH-0663", "Ergo Task Chair", "Ergoform", "T3", ProductCategory.FURNITURE,
                "Mesh task chair, adjustable lumbar", new BigDecimal("17.50"), new BigDecimal("749.00"), new BigDecimal("310.00"));
        product(organizationId, "NWR-SH-0210", "Trailhead 32L Pack", "Trailhead", "32", ProductCategory.SPORTS,
                "Hiking backpack, 32 litre", new BigDecimal("1.35"), new BigDecimal("189.00"), new BigDecimal("96.00"));
        product(organizationId, "NWR-TO-0333", "Novi Build Kit", "Novi", "BuildKit", ProductCategory.TOYS,
                "Educational construction set", new BigDecimal("1.90"), new BigDecimal("89.00"), new BigDecimal("41.00"));
        product(organizationId, "NWR-BK-0777", "Field Guide Series", "Pressmark", "Vol12", ProductCategory.BOOKS,
                "Hardcover reference volume", new BigDecimal("0.85"), new BigDecimal("54.00"), new BigDecimal("24.00"));
        product(organizationId, "NWR-GA-0404", "Verdant Raised Bed", "Verdant", "RB120", ProductCategory.GARDEN,
                "Composite raised bed, 120 cm", new BigDecimal("24.00"), new BigDecimal("329.00"), new BigDecimal("142.00"));
        product(organizationId, "NWR-CM-0901", "Core Wireless Mouse", "Core", "W1", ProductCategory.ELECTRONICS,
                "Wireless mouse, USB-C", new BigDecimal("0.12"), new BigDecimal("69.00"), new BigDecimal("38.00"));
        product(organizationId, "NWR-AP-0555", "Meridian Hoodie", "Meridian", "HD", ProductCategory.APPAREL,
                "Heavyweight cotton hoodie", new BigDecimal("0.75"), new BigDecimal("119.00"), new BigDecimal("45.00"));
    }

    private void product(java.util.UUID organizationId, String sku, String name, String brand, String model,
                        ProductCategory category, String description, BigDecimal weight, BigDecimal original,
                        BigDecimal resale) {
        Product product = new Product();
        product.setOrganizationId(organizationId);
        product.setSku(sku);
        product.setName(name);
        product.setBrand(brand);
        product.setModel(model);
        product.setCategory(category);
        product.setDescription(description);
        product.setWeightKg(weight);
        product.setOriginalPrice(original);
        product.setResaleValue(resale);
        product.setActive(true);
        productRepository.save(product);
    }

    private void seedLocations(java.util.UUID organizationId) {
        location(organizationId, "Newark Fulfillment Hub", LocationType.WAREHOUSE, "14 Logistics Way, Newark, NJ",
                "40.7357000", "-74.1724000", "US-NORTHEAST", 5000);
        location(organizationId, "Columbus Returns Centre", LocationType.WAREHOUSE, "880 Storage Pkwy, Columbus, OH",
                "39.9612000", "-82.9988000", "US-MIDWEST", 3200);
        location(organizationId, "Reno West Depot", LocationType.WAREHOUSE, "2210 Silver Lake Rd, Reno, NV",
                "39.4754000", "-119.8135000", "US-WEST", 1800);
        location(organizationId, "Meridian Repair Hub", LocationType.PARTNER_FACILITY, "55 Foundry St, Austin, TX",
                "30.2672000", "-97.7431000", "US-SOUTH", 900);
        location(organizationId, "Loop Resale Studio", LocationType.PARTNER_FACILITY, "12 Market Row, Boston, MA",
                "42.3601000", "-71.0589000", "US-NORTHEAST", 600);
    }

    private void location(java.util.UUID organizationId, String name, LocationType type, String address,
                          String latitude, String longitude, String region, int capacity) {
        Location location = new Location();
        location.setOrganizationId(organizationId);
        location.setName(name);
        location.setLocationType(type);
        location.setAddress(address);
        location.setLatitude(new BigDecimal(latitude));
        location.setLongitude(new BigDecimal(longitude));
        location.setRegion(region);
        location.setCapacityUnits(capacity);
        location.setActive(true);
        locationRepository.save(location);
    }

    private void seedFleet(java.util.UUID organizationId) {
        vehicle(organizationId, "Sprinter 12", "NJ-VAN-118", 140, new BigDecimal("900.00"));
        vehicle(organizationId, "Transit 24", "NJ-VAN-204", 260, new BigDecimal("1500.00"));
        vehicle(organizationId, "Box Truck 08", "OH-TRK-331", 90, new BigDecimal("3200.00"));
        driver(organizationId, "Marcus Hale", "+1 201 555 0143", "marcus.hale@northwind-retail.example");
        driver(organizationId, "Sofia Duarte", "+1 201 555 0188", "sofia.duarte@northwind-retail.example");
    }

    private void vehicle(java.util.UUID organizationId, String name, String registration, int capacity,
                         BigDecimal capacityKg) {
        Vehicle vehicle = new Vehicle();
        vehicle.setOrganizationId(organizationId);
        vehicle.setName(name);
        vehicle.setRegistration(registration);
        vehicle.setVehicleType(name.contains("Truck") ? "BOX_TRUCK" : "VAN");
        vehicle.setCapacityUnits(capacity);
        vehicle.setCapacityKg(capacityKg);
        vehicle.setActive(true);
        vehicleRepository.save(vehicle);
    }

    private void driver(java.util.UUID organizationId, String name, String phone, String email) {
        Driver driver = new Driver();
        driver.setOrganizationId(organizationId);
        driver.setName(name);
        driver.setPhone(phone);
        driver.setEmail(email);
        driver.setActive(true);
        driverRepository.save(driver);
    }

    private void seedPartners(java.util.UUID organizationId) {
        partner(organizationId, "Meridian Repair Hub", PartnerType.REPAIR,
                "Certified multi-brand repair centre handling phones, laptops and small appliances.",
                "Elena Voss", "elena.voss@meridianrepair.example", "+1 512 555 0110",
                "US-SOUTH,US-MIDWEST", "MOBILE_PHONES,LAPTOPS,ELECTRONICS,APPLIANCES",
                120, new BigDecimal("14.50"), null, true,
                "29.8000000", "-97.7000000", "55 Foundry St, Austin, TX");
        partner(organizationId, "Loop Resale Studio", PartnerType.REFURBISHMENT,
                "Refurbishment and graded open-box resale for consumer electronics and small appliances.",
                "Daniel Reinhart", "daniel@loopresale.example", "+1 617 555 0177",
                "US-NORTHEAST", "MOBILE_PHONES,LAPTOPS,ELECTRONICS,TOYS,BOOKS",
                200, null, new BigDecimal("11.00"), true,
                "42.3612000", "-71.0552000", "12 Market Row, Boston, MA");
        partner(organizationId, "Cascade Liquidation Partners", PartnerType.LIQUIDATION,
                "Wholesale liquidation of working consumer goods for resellers and marketplaces.",
                "Ruth Altman", "ruth@cascadeliquidation.example", "+1 312 555 0192",
                "US-MIDWEST,US-NORTHEAST,US-WEST", "MOBILE_PHONES,LAPTOPS,ELECTRONICS,APPLIANCES,SPORTS",
                800, new BigDecimal("4.20"), null, true,
                "41.8800000", "-87.6300000", "300 W Fulton Market, Chicago, IL");
        partner(organizationId, "GreenCycle Materials", PartnerType.RECYCLING,
                "Certified electronics and plastics recycler handling parts harvesting and material recovery.",
                "Henrik Baumann", "henrik@greencycle.example", "+1 775 555 0143",
                "US-WEST,US-MIDWEST,US-NORTHEAST,US-SOUTH", "ELECTRONICS,APPLIANCES,MOBILE_PHONES,LAPTOPS",
                1500, new BigDecimal("2.10"), null, true,
                "39.5296000", "-119.8138000", "6000 Silver Lake Rd, Reno, NV");
        partner(organizationId, "Switchback Logistics", PartnerType.LOGISTICS,
                "Regional line-haul and last-mile carrier for reverse logistics movements.",
                "Imani Cole", "imani@switchbacklogistics.example", "+1 646 555 0155",
                "US-NORTHEAST,US-MIDWEST", "OTHER",
                400, new BigDecimal("6.00"), null, false,
                "40.7505000", "-73.9934000", "250 W 34th St, New York, NY");
    }

    private void partner(java.util.UUID organizationId, String name, PartnerType type, String description,
                         String contactName, String contactEmail, String phone, String regions, String categories,
                         int capacity, BigDecimal feePerUnit, BigDecimal feePercentage, boolean available,
                         String latitude, String longitude, String address) {
        Partner partner = new Partner();
        partner.setOrganizationId(organizationId);
        partner.setName(name);
        partner.setPartnerType(type);
        partner.setDescription(description);
        partner.setContactName(contactName);
        partner.setContactEmail(contactEmail);
        partner.setContactPhone(phone);
        partner.setServiceableRegions(regions);
        partner.setSupportedCategories(categories);
        partner.setProcessingCapacityPerDay(capacity);
        partner.setCurrentlyAvailable(available);
        partner.setFeePerUnit(feePerUnit);
        partner.setFeePercentage(feePercentage);
        partner.setAcceptanceCriteria("Goods must be physically complete, correctly logged and free of "
                + "hazardous materials. Items outside the declared category list are rejected on arrival.");
        partner.setLatitude(new BigDecimal(latitude));
        partner.setLongitude(new BigDecimal(longitude));
        partner.setAddress(address);
        partner.setVerified(true);
        partner.setVerifiedAt(java.time.Instant.now());
        partnerRepository.save(partner);
    }
}
