package com.da.billiards.config;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.da.billiards.article.Article;
import com.da.billiards.article.ArticleRepository;
import com.da.billiards.catalog.Brand;
import com.da.billiards.catalog.BrandRepository;
import com.da.billiards.catalog.Category;
import com.da.billiards.catalog.CategoryRepository;
import com.da.billiards.catalog.Product;
import com.da.billiards.catalog.ProductRepository;
import com.da.billiards.catalog.ProductSpec;
import com.da.billiards.catalog.ProductVariant;
import com.da.billiards.common.SlugUtil;
import com.da.billiards.user.Role;
import com.da.billiards.user.User;
import com.da.billiards.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tạo tài khoản quản trị lần đầu và (tuỳ cấu hình) dữ liệu mẫu để chạy thử.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private static final String IMAGES = "/images/";

    private final AppProperties properties;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final ArticleRepository articleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedAdmin();
        if (properties.seedSampleData() && categoryRepository.count() == 0) {
            seedCatalog();
            seedArticles();
            log.info("Đã tạo dữ liệu mẫu cho cửa hàng");
        }
    }

    private void seedAdmin() {
        AppProperties.Admin admin = properties.admin();
        if (admin == null || admin.email() == null || admin.email().isBlank()) {
            return;
        }
        String email = admin.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        if (admin.password() == null || admin.password().length() < 8) {
            log.warn("Chưa tạo tài khoản admin: hãy đặt ADMIN_PASSWORD (tối thiểu 8 ký tự)");
            return;
        }
        User user = new User();
        user.setFullName("Quản trị viên");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(admin.password()));
        user.setRole(Role.ADMIN);
        userRepository.save(user);
        log.info("Đã tạo tài khoản admin {}", email);
    }

    private void seedCatalog() {
        Category cues = category("Cơ bida", null, 1, "Cơ lỗ, cơ carom, cơ phá và cơ nhảy");
        Category poolCues = category("Cơ lỗ", cues, 1, null);
        Category caromCues = category("Cơ carom", cues, 2, null);
        Category breakCues = category("Cơ phá", cues, 3, null);
        Category jumpCues = category("Cơ nhảy", cues, 4, null);
        Category tables = category("Bàn bida", null, 2, "Bàn bida lỗ và bàn carom cho gia đình, câu lạc bộ");
        Category accessories = category("Phụ kiện", null, 3, "Đầu cơ, phấn, găng tay, bao hộp cơ");
        Category tips = category("Đầu cơ", accessories, 1, null);
        Category chalk = category("Phấn bida", accessories, 2, null);
        Category gloves = category("Găng tay", accessories, 3, null);
        Category cases = category("Bao & hộp cơ", accessories, 4, null);
        Category balls = category("Bi bida", null, 4, "Bộ bi lỗ và bi carom");
        Category poolBalls = category("Bi lỗ", balls, 1, "Bộ 16 bi cho bàn lỗ, đường kính 57.2mm",
                IMAGES + "products/aramith-black-1.webp");
        Category caromBalls = category("Bi carom", balls, 2, "Bộ 3 bi cho bàn carom và 3 băng",
                IMAGES + "products/balls-4.jpg");

        Brand falcon = brand("Falcon", "Cơ thi đấu ngọn gỗ phong, độ thẳng cao");
        Brand kaiser = brand("Kaiser Cue", "Dòng cơ và phụ kiện giá tốt cho người mới chơi");
        Brand aria = brand("Aria", "Chuyên cơ carom và phấn thi đấu");
        Brand titan = brand("Titan Tables", "Bàn bida khung gỗ đặc, mặt đá liền");
        Brand lotus = brand("Lotus Billiards", "Bàn carom và hộp cơ thủ công");

        product("Cơ lỗ Falcon F-10", poolCues, falcon, true,
                "Cơ lỗ tầm trung, ngọn gỗ phong 12.5mm, khớp nối nhanh.", images("cue-1", "cue-2", "play-1"),
                List.of(spec("Chiều dài", "147 cm"), spec("Đường kính ngọn", "12.5 mm"),
                        spec("Chất liệu ngọn", "Gỗ phong Canada"), spec("Khớp nối", "Radial pin")),
                variant("19oz", 3_200_000, 2_890_000L, 8), variant("20oz", 3_200_000, 2_890_000L, 5));
        product("Cơ lỗ Kaiser Classic", poolCues, kaiser, false,
                "Lựa chọn hợp lý cho người mới bắt đầu, cán quấn chỉ chống trơn.", images("cue-2", "play-1"),
                List.of(spec("Chiều dài", "147 cm"), spec("Đường kính ngọn", "13 mm"), spec("Cán", "Quấn chỉ Ireland")),
                variant("19oz", 1_850_000, null, 12), variant("20oz", 1_850_000, null, 10),
                variant("21oz", 1_850_000, null, 4));
        product("Cơ lỗ Falcon Carbon X", poolCues, falcon, true,
                "Ngọn carbon giảm lệch bi, phù hợp người chơi thi đấu.", images("cue-8", "cue-3", "play-2"),
                List.of(spec("Chiều dài", "148 cm"), spec("Đường kính ngọn", "12.4 mm"),
                        spec("Chất liệu ngọn", "Sợi carbon"), spec("Bảo hành", "24 tháng")),
                variant("19oz", 12_500_000, null, 3), variant("20oz", 12_500_000, null, 2));
        product("Cơ carom Aria 3C Pro", caromCues, aria, true,
                "Cơ 3 băng ngọn 11.8mm, độ cứng vừa, đánh xoáy tốt.", images("cue-4", "cue-5"),
                List.of(spec("Chiều dài", "142 cm"), spec("Đường kính ngọn", "11.8 mm"), spec("Trọng lượng", "18.5oz")),
                variant("Ngọn 11.8mm", 4_800_000, 4_390_000L, 6), variant("Ngọn 12mm", 4_800_000, 4_390_000L, 4));
        product("Cơ phá Titan Breaker", breakCues, titan, false,
                "Đầu cơ phenolic cứng, truyền lực mạnh cho cú phá.", images("cue-3", "balls-1"),
                List.of(spec("Đầu cơ", "Phenolic"), spec("Trọng lượng", "19oz"), spec("Ngọn", "13 mm")),
                variant("Tiêu chuẩn", 2_400_000, null, 7));
        product("Cơ nhảy Kaiser Jump", jumpCues, kaiser, false,
                "Cơ nhảy ngắn, nhẹ, tháo rời được.", images("cue-5", "cue-6"),
                List.of(spec("Chiều dài", "104 cm"), spec("Trọng lượng", "8oz")),
                variant("Tiêu chuẩn", 1_600_000, 1_450_000L, 9));
        product("Bàn bida lỗ Titan Pro", tables, titan, true,
                "Mặt đá liền 3 tấm, nỉ thi đấu, khung gỗ sồi.", images("table-2", "table-1", "table-4"),
                List.of(spec("Mặt đá", "Đá phiến 25mm"), spec("Nỉ", "Nỉ len thi đấu"),
                        spec("Bảo hành", "5 năm khung bàn"), spec("Giao hàng", "Lắp đặt tận nơi")),
                variant("8 feet", 58_000_000, null, 2), variant("9 feet", 65_000_000, null, 2));
        product("Bàn carom Lotus Classic", tables, lotus, false,
                "Bàn carom mặt đá có sưởi, băng cao su chuẩn.", images("table-5", "table-3"),
                List.of(spec("Kích thước", "2.84m x 1.42m"), spec("Sưởi mặt bàn", "Có"), spec("Màu nỉ", "Xanh dương")),
                variant("Tiêu chuẩn", 72_000_000, 68_000_000L, 1));
        product("Đầu cơ Kaiser Layer", tips, kaiser, false,
                "Đầu cơ da heo nhiều lớp, giữ phấn tốt.", images("tip-1", "cue-1"),
                List.of(spec("Đường kính", "14 mm"), spec("Số lớp", "8 lớp")),
                variant("Mềm (S)", 350_000, null, 30), variant("Vừa (M)", 350_000, null, 25),
                variant("Cứng (H)", 350_000, null, 3));
        product("Phấn bida Aria Blue", chalk, aria, false,
                "Phấn ít bụi, bám đầu cơ lâu.", images("chalk-1"),
                List.of(spec("Quy cách", "Hộp 2 viên"), spec("Màu", "Xanh dương")),
                variant("Hộp 2 viên", 120_000, null, 50), variant("Hộp 12 viên", 650_000, 590_000L, 10));
        product("Găng tay bida Falcon 3 ngón", gloves, falcon, false,
                "Vải co giãn thoáng khí, giúp cơ trượt đều.", images("glove-1", "play-2"),
                List.of(spec("Chất liệu", "Lycra"), spec("Size", "Free size")),
                variant("Tay trái", 150_000, null, 40), variant("Tay phải", 150_000, null, 15));
        product("Hộp cơ Lotus 2x4", cases, lotus, false,
                "Hộp da cứng đựng 2 cán 4 ngọn, có ngăn phụ kiện.", images("cue-7"),
                List.of(spec("Sức chứa", "2 cán, 4 ngọn"), spec("Chất liệu", "Da PU, khung nhôm")),
                variant("Đen", 1_450_000, null, 6), variant("Nâu", 1_450_000, null, 4));
        product("Bộ bi lỗ Aria Tournament", poolBalls, aria, true,
                "Bộ 16 bi nhựa phenolic, đường kính 57.2mm.", images("balls-1", "balls-3", "balls-6"),
                List.of(spec("Số lượng", "16 bi"), spec("Đường kính", "57.2 mm"), spec("Chất liệu", "Phenolic")),
                variant("Tiêu chuẩn", 3_600_000, 3_290_000L, 5));

        product("Bộ bi lỗ Kaiser Practice", poolBalls, kaiser, false,
                "Bộ bi tập luyện giá tốt, màu sắc rõ ràng.", images("balls-2", "balls-5"),
                List.of(spec("Số lượng", "16 bi"), spec("Đường kính", "57.2 mm"), spec("Chất liệu", "Polyester")),
                variant("Tiêu chuẩn", 1_250_000, 990_000L, 14));
        product("Bộ bi carom Aria 3 bi", caromBalls, aria, false,
                "Bộ 3 bi carom đường kính 61.5mm cho bàn 3 băng.", images("balls-4"),
                List.of(spec("Số lượng", "3 bi"), spec("Đường kính", "61.5 mm")),
                variant("Tiêu chuẩn", 2_100_000, null, 8));
        product("Bàn bida lỗ Kaiser Club", tables, kaiser, false,
                "Bàn cho câu lạc bộ, khung gỗ ép chống cong vênh.", images("table-6", "table-4"),
                List.of(spec("Mặt đá", "Đá phiến 19mm"), spec("Nỉ", "Nỉ pha len"), spec("Bảo hành", "3 năm")),
                variant("8 feet", 32_000_000, 29_500_000L, 3), variant("9 feet", 36_000_000, 33_000_000L, 2));
        product("Bàn bida lỗ Titan Home 7 feet", tables, titan, false,
                "Kích thước gọn cho phòng giải trí tại nhà.", images("table-3", "table-1"),
                List.of(spec("Kích thước", "7 feet"), spec("Mặt đá", "Đá phiến 19mm"), spec("Màu nỉ", "Xanh lá")),
                variant("Khung nâu", 24_500_000, null, 4), variant("Khung đen", 25_500_000, null, 2));
        product("Cơ carom Falcon Libre", caromCues, falcon, false,
                "Cơ carom ngọn gỗ phong, hợp lối đánh 1 băng và libre.", images("cue-4", "cue-8"),
                List.of(spec("Chiều dài", "142 cm"), spec("Đường kính ngọn", "12 mm"), spec("Trọng lượng", "18oz")),
                variant("Tiêu chuẩn", 3_500_000, 3_150_000L, 5));
        product("Cơ lỗ Aria Nova", poolCues, aria, false,
                "Cơ lỗ thiết kế tối giản, cán gỗ mun phối bạc.", images("cue-6", "cue-2"),
                List.of(spec("Chiều dài", "147 cm"), spec("Đường kính ngọn", "12.75 mm"), spec("Cán", "Gỗ mun")),
                variant("19oz", 5_600_000, null, 6), variant("20oz", 5_600_000, null, 3));
        product("Bàn bida lỗ Lotus Heritage", tables, lotus, true,
                "Bàn chân tiện thủ công, nỉ đỏ rượu vang.", images("table-1", "table-2"),
                List.of(spec("Kích thước", "8 feet"), spec("Mặt đá", "Đá phiến 25mm"), spec("Màu nỉ", "Đỏ rượu vang")),
                variant("8 feet", 78_000_000, 72_000_000L, 1));

        seedBrandCollections(poolCues, caromCues, breakCues, cues, tips, chalk, cases, poolBalls);
    }

    /**
     * Các dòng sản phẩm của những thương hiệu phổ biến tại Việt Nam. Ảnh là ảnh chụp thật có giấy phép mở
     * (xem CREDITS.md) nhưng không đúng từng mẫu; giá và thông số chỉ là dữ liệu mẫu để chạy thử.
     * Tạo sau cùng nên đây cũng là nhóm "Hàng mới về".
     */
    private void seedBrandCollections(Category poolCues, Category caromCues, Category breakCues, Category cues,
                                      Category tips, Category chalk, Category cases, Category poolBalls) {
        Category shafts = category("Ngọn cơ", cues, 5, null, IMAGES + "products/predator-revo.jpg");

        Brand peri = brand("Peri", "Thương hiệu cơ Đài Loan, nhiều dòng từ phổ thông tới thi đấu");
        Brand cuetec = brand("Cuetec", "Thương hiệu Mỹ, nổi tiếng với dòng ngọn carbon Cynergy");
        Brand fury = brand("Fury", "Cơ Đài Loan giá hợp lý, hoàn thiện đẹp");
        Brand mezz = brand("Mezz", "Thương hiệu Nhật Bản, mạnh về cơ carom");
        Brand predator = brand("Predator", "Ngọn ít lệch bi, cơ phá và cơ thi đấu cao cấp");
        Brand aramith = brand("Aramith", "Bi bida phenolic sản xuất tại Bỉ");
        Brand kamui = brand("Kamui", "Đầu cơ da heo nhiều lớp của Nhật Bản");
        Brand taom = brand("Taom", "Phấn bida thế hệ mới từ Phần Lan");
        Brand dynaspheres = brand("Dynaspheres", "Bi nhựa phenolic thế hệ mới, kiểm định chất lượng tại Bỉ");
        Brand diamond = brand("Diamond", "Thương hiệu bàn và phụ kiện bida của Mỹ");

        product("Cơ lỗ Peri STV-02", poolCues, peri, true,
                "Thân cơ đen bóng, mũi trang trí đỏ, ngọn gỗ phong 12.75mm. Cân bằng tốt cho cả đánh tập lẫn thi đấu phong trào.",
                images("peri-stv-02", "play-1"),
                List.of(spec("Chiều dài", "147 cm"), spec("Đường kính ngọn", "12.75 mm"), spec("Cán", "Quấn da"),
                        spec("Khớp nối", "Uni-loc"), spec("Đầu cơ", "Da heo nhiều lớp")),
                variant("19oz", 6_900_000, 6_500_000L, 6), variant("20oz", 6_900_000, 6_500_000L, 4));
        product("Cơ lỗ Peri P-D05", poolCues, peri, false,
                "Thân gỗ đỏ phối mũi vàng, đi kèm ngọn carbon giảm lệch bi khi đánh xoáy.",
                images("peri-p-d05", "cue-8"),
                List.of(spec("Chiều dài", "147 cm"), spec("Đường kính ngọn", "12.5 mm"), spec("Ngọn", "Carbon"),
                        spec("Cán", "Quấn chỉ")),
                variant("19oz", 7_200_000, null, 5), variant("20oz", 7_200_000, null, 3));
        product("Cơ lỗ Peri RD-03", poolCues, peri, false,
                "Phối màu trắng ngọc – xanh dương, giá dễ tiếp cận cho người mới chơi.",
                images("peri-rd-03", "cue-2"),
                List.of(spec("Chiều dài", "147 cm"), spec("Đường kính ngọn", "13 mm"), spec("Cán", "Quấn chỉ")),
                variant("19oz", 3_950_000, 3_590_000L, 10), variant("20oz", 3_950_000, 3_590_000L, 8));
        product("Cơ lỗ Cuetec Cynergy Carbon", poolCues, cuetec, true,
                "Ngọn carbon Cynergy, thân đen mờ phối bạc. Độ ổn định cao, ít lệch bi ở các cú đánh mạnh.",
                images("cuetec-cynergy", "play-2"),
                List.of(spec("Chiều dài", "147 cm"), spec("Đường kính ngọn", "12.5 mm"), spec("Ngọn", "Carbon Cynergy"),
                        spec("Khớp nối", "Radial"), spec("Bảo hành", "12 tháng")),
                variant("19oz", 18_900_000, null, 3), variant("20oz", 18_900_000, null, 2));
        product("Cơ lỗ Cuetec AVID Chrome", poolCues, cuetec, false,
                "Thân xám ánh kim, cán quấn chỉ chống trơn, ngọn gỗ phong xử lý chống ẩm.",
                images("cuetec-avid", "cue-3"),
                List.of(spec("Chiều dài", "147 cm"), spec("Đường kính ngọn", "12.75 mm"), spec("Cán", "Quấn chỉ")),
                variant("19oz", 9_800_000, 8_900_000L, 4), variant("20oz", 9_800_000, 8_900_000L, 4));
        product("Cơ lỗ Fury AS-3", poolCues, fury, false,
                "Gỗ mun phối mũi xanh lục và khảm vàng, hoàn thiện sơn bóng nhiều lớp.",
                images("fury-as-3", "cue-1"),
                List.of(spec("Chiều dài", "147 cm"), spec("Đường kính ngọn", "12.75 mm"), spec("Cán", "Quấn da")),
                variant("19oz", 5_900_000, null, 6), variant("20oz", 5_900_000, null, 5));
        product("Cơ lỗ Fury TW-4", poolCues, fury, false,
                "Thân đỏ đô phối mũi đen, cán quấn chỉ, hợp người chơi thích cảm giác cơ đầm tay.",
                images("fury-tw-4", "cue-6"),
                List.of(spec("Chiều dài", "147 cm"), spec("Đường kính ngọn", "13 mm"), spec("Cán", "Quấn chỉ")),
                variant("19oz", 4_500_000, 4_200_000L, 7), variant("20oz", 4_500_000, 4_200_000L, 6));
        product("Cơ carom Mezz EC7", caromCues, mezz, true,
                "Cơ 3 băng thân gỗ phong vân mắt chim, ngọn 11.75mm cho điểm chạm chính xác.",
                images("mezz-ec7", "cue-4"),
                List.of(spec("Chiều dài", "141 cm"), spec("Đường kính ngọn", "11.75 mm"), spec("Trọng lượng", "18.5oz"),
                        spec("Khớp nối", "Wavy joint")),
                variant("Ngọn 11.75mm", 21_500_000, null, 2), variant("Ngọn 12mm", 21_500_000, null, 2));
        product("Cơ phá Predator BK Rush", breakCues, predator, true,
                "Ngọn carbon và đầu cơ cứng giúp truyền lực tối đa, kiểm soát bi cái tốt sau cú phá.",
                images("predator-bk-rush", "balls-1"),
                List.of(spec("Đầu cơ", "Đầu phá cứng"), spec("Ngọn", "Carbon"), spec("Trọng lượng", "19oz")),
                variant("Tiêu chuẩn", 16_500_000, 15_900_000L, 3));
        product("Ngọn carbon Predator REVO 12.4", shafts, predator, true,
                "Ngọn carbon thay thế cho cơ lỗ, độ lệch bi thấp và dễ vệ sinh.",
                images("predator-revo", "cue-8"),
                List.of(spec("Đường kính", "12.4 mm"), spec("Chiều dài", "75 cm"), spec("Chất liệu", "Sợi carbon")),
                variant("Khớp Uni-loc", 15_900_000, null, 3), variant("Khớp Radial", 15_900_000, null, 2));
        product("Bộ bi lỗ Aramith Tournament Black", poolBalls, aramith, true,
                "Bi số sọc đen đặc trưng, nhìn rõ bi trên bàn khi thi đấu và quay hình. Bi cái có logo Aramith.",
                images("aramith-black-1.webp", "aramith-black-2.webp"),
                List.of(spec("Số lượng", "16 bi"), spec("Đường kính", "57.2 mm"), spec("Chất liệu", "Nhựa phenolic"),
                        spec("Xuất xứ", "Bỉ")),
                variant("Tiêu chuẩn", 7_500_000, null, 4));
        product("Bộ bi lỗ Dynaspheres Palladium", poolBalls, dynaspheres, true,
                "Số in trong ô tam giác dễ đọc, bi cái có ba chấm định vị để luyện xoáy. Bi lăn đều, ít bám bẩn.",
                images("dyna-palladium-1.webp", "dyna-palladium-2.webp"),
                List.of(spec("Số lượng", "16 bi"), spec("Đường kính", "57.2 mm"), spec("Chất liệu", "Nhựa phenolic"),
                        spec("Bi cái", "3 chấm định vị")),
                variant("Tiêu chuẩn", 4_200_000, 3_890_000L, 6));
        product("Bộ bi lỗ Diamond Ultra C", poolBalls, diamond, false,
                "Màu bi chuẩn, mặt bi bóng. Bi cái chấm đỏ giúp nhìn rõ độ xoáy, hợp bàn câu lạc bộ và gia đình.",
                images("diamond-ultra-c-1.webp", "diamond-ultra-c-2.webp"),
                List.of(spec("Số lượng", "16 bi"), spec("Đường kính", "57.2 mm"), spec("Chất liệu", "Nhựa phenolic"),
                        spec("Bi cái", "Chấm đỏ")),
                variant("Tiêu chuẩn", 3_000_000, 2_790_000L, 8));
        product("Bộ bi Aramith kỷ niệm 100 năm", poolBalls, aramith, true,
                "Phiên bản kỷ niệm 100 năm Aramith (1923 – 2023), đựng trong hộp gỗ lót nỉ xanh. Hợp để sưu tầm hoặc làm quà.",
                images("aramith-100-1.webp", "aramith-100-2.webp"),
                List.of(spec("Số lượng", "16 bi"), spec("Đường kính", "57.2 mm"), spec("Chất liệu", "Nhựa phenolic"),
                        spec("Hộp", "Hộp gỗ lót nỉ")),
                variant("Hộp gỗ", 10_000_000, null, 2));
        product("Đầu cơ Kamui Black", tips, kamui, false,
                "Đầu cơ da heo 10 lớp, giữ phấn lâu và giữ form tốt sau thời gian dài sử dụng.",
                images("kamui-black", "tip-1"),
                List.of(spec("Đường kính", "14 mm"), spec("Số lớp", "10 lớp")),
                variant("Mềm (S)", 690_000, null, 20), variant("Vừa (M)", 690_000, null, 20),
                variant("Cứng (H)", 690_000, null, 12));
        product("Phấn bida Taom Pyro", chalk, taom, false,
                "Phấn ít bụi, không bám bẩn nỉ, chỉ cần một lần lau phấn là đủ cho nhiều cú đánh.",
                images("taom-pyro", "chalk-1"),
                List.of(spec("Quy cách", "1 viên"), spec("Màu", "Đen")),
                variant("1 viên", 450_000, null, 30));
        product("Hộp cơ Fury 2x4", cases, fury, false,
                "Hộp ống cứng chứa 2 cán 4 ngọn, có quai đeo và ngăn để phụ kiện.",
                images("fury-case-2x4", "cue-7"),
                List.of(spec("Sức chứa", "2 cán, 4 ngọn"), spec("Chất liệu", "Da PU, lót nhung")),
                variant("Đen", 2_350_000, 2_100_000L, 5));
    }

    private void seedArticles() {
        article("Ngọn carbon hay ngọn gỗ: nên chọn loại nào?", "products/predator-revo.jpg", 0,
                "So sánh độ lệch bi, cảm giác đánh và chi phí để chọn đúng ngọn cơ cho lối chơi của bạn.",
                """
                Khi đánh bi cái lệch tâm, ngọn cơ bị uốn và đẩy bi cái lệch khỏi hướng ngắm một chút. Hiện tượng này gọi là \
                lệch bi (deflection). Ngọn càng nhẹ ở phần đầu thì bi càng ít lệch.

                Ngọn carbon có phần đầu nhẹ, độ cứng đồng đều và gần như không cong vênh theo độ ẩm. Người chơi xoáy nhiều \
                thường thấy ngắm dễ hơn với ngọn carbon. Đổi lại, giá cao hơn và cảm giác đánh "đanh" hơn.

                Ngọn gỗ phong cho cảm giác mềm, phản hồi rõ khi chạm bi, giá dễ chịu và dễ thay đầu cơ. Ngọn gỗ cần được \
                bảo quản kỹ hơn, tránh nơi ẩm và nắng nóng.

                Gợi ý: nếu mới chơi, hãy bắt đầu với ngọn gỗ để làm quen cảm giác. Khi đã ổn định kỹ thuật và hay dùng xoáy, \
                ngọn carbon là khoản đầu tư đáng cân nhắc.""");
        article("5 lỗi thường gặp khi mới tập bida lỗ", "products/play-2.jpg", 2,
                "Tư thế, cầu tay, nhịp đánh… những thói quen nhỏ quyết định độ chính xác của cú đánh.",
                """
                1. Cầu tay không chắc. Cầu tay lỏng làm ngọn cơ rung khi đẩy. Hãy ép lòng bàn tay xuống nỉ và giữ khoảng cách \
                từ cầu tay tới bi cái khoảng 15 đến 20 cm.

                2. Nâng người khi đánh. Nhiều người ngẩng đầu ngay lúc ra cơ để xem bi đi. Hãy giữ yên tư thế cho tới khi bi \
                cái rời khỏi đầu cơ.

                3. Cầm cơ quá chặt. Tay sau chỉ nên giữ cơ như cầm một chiếc bút, để cổ tay thả lỏng.

                4. Đánh quá mạnh. Phần lớn cú đánh chỉ cần lực vừa phải. Lực mạnh làm khó kiểm soát vị trí bi cái.

                5. Không nhắm trước khi cúi xuống. Hãy đứng sau bi cái, xác định đường bi rồi mới vào tư thế.""");
        article("Cách thay đầu cơ bida tại nhà", "products/tip-1.jpg", 4,
                "Dụng cụ cần chuẩn bị và các bước thay, gọt đầu cơ đúng kỹ thuật.",
                """
                Đầu cơ bị chai, mòn lệch hoặc không còn giữ phấn là lúc cần thay. Bạn cần: đầu cơ mới đúng đường kính, keo \
                dán chuyên dụng, dao rọc giấy, giấy nhám mịn và dụng cụ gọt tròn đầu cơ.

                Bước 1: cắt bỏ đầu cơ cũ sát mặt ferrule, chà phẳng nhẹ bằng giấy nhám.

                Bước 2: bôi một lớp keo mỏng, đặt đầu cơ mới vào giữa và giữ chặt khoảng một phút. Để keo khô hẳn theo hướng \
                dẫn của nhà sản xuất.

                Bước 3: cắt phần thừa quanh mép, gọt tròn mặt đầu cơ theo dạng đồng xu (dime) hoặc nửa đồng xu tuỳ thói quen.

                Nếu không tự tin, bạn có thể mang cơ tới cửa hàng để được thay miễn phí khi mua đầu cơ.""");
        article("Phấn bida: vì sao loại tốt lại quan trọng?", "products/taom-pyro.jpg", 6,
                "Phấn giúp đầu cơ bám bi, giảm trượt cơ. Cách đánh phấn đúng và chọn phấn phù hợp.",
                """
                Phấn tạo ma sát giữa đầu cơ và bi cái. Thiếu phấn hoặc phấn kém chất lượng là nguyên nhân phổ biến của \
                lỗi trượt cơ (miscue), nhất là khi đánh xoáy.

                Phấn tốt bám đều, ít bụi và không làm bẩn nỉ hay bi. Một số dòng phấn thế hệ mới chỉ cần đánh nhẹ là đủ \
                cho vài cú đánh liên tiếp.

                Khi đánh phấn, hãy xoay viên phấn nhẹ quanh mặt đầu cơ thay vì miết mạnh như mài. Đánh phấn trước mỗi cú \
                đánh quan trọng.""");
        article("Chọn trọng lượng cơ bida thế nào cho phù hợp?", "articles/chon-trong-luong-co.jpg", 8,
                "19oz hay 20oz? Một vài gợi ý để chọn cơ theo lối đánh.",
                """
                Trọng lượng cơ phổ biến nằm trong khoảng 18 đến 21oz. Cơ nhẹ giúp kiểm soát bi tốt hơn ở những cú đánh nhẹ, \
                còn cơ nặng cho lực đi bi ổn định hơn.

                Nếu mới chơi, bạn nên bắt đầu với cơ 19oz. Sau một thời gian, hãy thử thêm cơ 20oz để cảm nhận sự khác biệt.

                Ngoài trọng lượng, đường kính ngọn cũng ảnh hưởng tới cảm giác đánh: ngọn nhỏ dễ tạo xoáy, ngọn lớn dễ ngắm hơn.""");
        article("Bảo quản cơ bida đúng cách", "articles/bao-quan-co.jpg", 10,
                "Những thói quen nhỏ giúp cơ luôn thẳng và bền.",
                """
                Không để cơ dựng nghiêng vào tường trong thời gian dài, vì ngọn có thể bị cong. Hãy cất cơ trong hộp hoặc giá treo.

                Tránh để cơ ở nơi ẩm hoặc gần nguồn nhiệt. Gỗ phong rất nhạy với độ ẩm.

                Sau mỗi buổi chơi, lau ngọn cơ bằng khăn mềm, khô để loại bỏ phấn và mồ hôi tay.""");
        article("Phân biệt bàn bida lỗ và bàn carom", "articles/ban-lo-va-ban-carom.jpg", 12,
                "Khác nhau về kích thước, bi và cách chơi.",
                """
                Bàn bida lỗ có 6 lỗ, thường dùng bộ 16 bi. Kích thước phổ biến là 8 feet và 9 feet.

                Bàn carom không có lỗ, chơi với 3 bi, mặt bàn thường có sưởi để bi lăn ổn định.

                Khi chọn bàn cho gia đình, hãy đo diện tích phòng: cần thêm khoảng 1,5m mỗi cạnh để có chỗ đứng đánh.""");
    }

    private Category category(String name, Category parent, int sortOrder, String description) {
        return category(name, parent, sortOrder, description, null);
    }

    private Category category(String name, Category parent, int sortOrder, String description, String imageUrl) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(SlugUtil.unique(name, categoryRepository::existsBySlug));
        category.setParent(parent);
        category.setSortOrder(sortOrder);
        category.setDescription(description);
        category.setImageUrl(imageUrl != null ? imageUrl : IMAGES + "categories/" + category.getSlug() + ".jpg");
        return categoryRepository.save(category);
    }

    private Brand brand(String name, String description) {
        Brand brand = new Brand();
        brand.setName(name);
        brand.setSlug(SlugUtil.slugify(name));
        brand.setDescription(description);
        return brandRepository.save(brand);
    }

    private void product(String name, Category category, Brand brand, boolean featured, String shortDescription,
                         List<String> images, List<ProductSpec> specs, ProductVariant... variants) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(SlugUtil.unique(name, productRepository::existsBySlug));
        product.setCategory(category);
        product.setBrand(brand);
        product.setFeatured(featured);
        product.setShortDescription(shortDescription);
        product.setDescription(shortDescription + "\n\nSản phẩm chính hãng, đổi trả trong 7 ngày nếu có lỗi từ nhà sản xuất.");
        product.setThumbnail(images.get(0));
        product.getImages().addAll(images);
        product.getSpecs().addAll(specs);
        for (ProductVariant variant : variants) {
            variant.setProduct(product);
            product.getVariants().add(variant);
        }
        product.refreshPricing();
        productRepository.save(product);
    }

    /** {@code image} tính từ thư mục /images; {@code daysAgo} giãn ngày đăng để bài mới nhất hiện đầu tiên. */
    private void article(String title, String image, int daysAgo, String summary, String content) {
        Article article = new Article();
        article.setTitle(title);
        article.setSlug(SlugUtil.unique(title, articleRepository::existsBySlug));
        article.setThumbnail(IMAGES + image);
        article.setSummary(summary);
        article.setContent(content);
        article.setPublished(true);
        article.setPublishedAt(LocalDateTime.now().minusDays(daysAgo));
        articleRepository.save(article);
    }

    /** Tên không có đuôi được hiểu là ảnh .jpg. */
    private static List<String> images(String... names) {
        return Arrays.stream(names)
                .map(name -> IMAGES + "products/" + (name.contains(".") ? name : name + ".jpg"))
                .toList();
    }

    private static ProductSpec spec(String name, String value) {
        return new ProductSpec(name, value);
    }

    private static ProductVariant variant(String name, long price, Long salePrice, int stock) {
        ProductVariant variant = new ProductVariant();
        variant.setName(name);
        variant.setPrice(price);
        variant.setSalePrice(salePrice);
        variant.setStock(stock);
        return variant;
    }
}
