package com.motorrent.motorbikerental.controller.api;
import com.motorrent.motorbikerental.security.CurrentUser;
import com.motorrent.motorbikerental.repository.*;
import com.motorrent.motorbikerental.mapper.UserMapper;
import jakarta.validation.Valid; import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate; import java.nio.file.*; import javax.imageio.ImageIO;
@RestController @RequestMapping("/api/account") @RequiredArgsConstructor
public class AccountController {
 private final CurrentUser current; private final UserRepository users; private final UserMapper mapper;
 private final PasswordEncoder encoder; private final TokenAuthenticationRepository tokens;
 public record Profile(@NotBlank @Size(max=100) String firstName,@NotBlank @Size(max=100) String lastName,
  @NotBlank @Email @Size(max=255) String email,@NotBlank @Pattern(regexp="[+0-9 ()-]{9,20}") String phone,
  @Size(max=255) String address,Boolean gender,@Past LocalDate birthDate) {}
 public record Password(@NotBlank String currentPassword,@Size(min=8,max=72) @NotBlank String newPassword,@NotBlank String confirmPassword) {}
 @GetMapping public Object get() { return mapper.toResponse(current.get()); }
 @PutMapping @Transactional public Object save(@Valid @RequestBody Profile p) {
  var u=current.get(); var existing=users.findByEmail(p.email().trim());
  if(existing.isPresent() && !existing.get().getId().equals(u.getId())) throw new IllegalArgumentException("Email đã được sử dụng");
  var phone=users.findByPhone(p.phone().trim());
  if(phone.isPresent() && !phone.get().getId().equals(u.getId())) throw new IllegalArgumentException("Số điện thoại đã được sử dụng");
  u.setFirstName(p.firstName().trim());u.setLastName(p.lastName().trim());u.setEmail(p.email().trim());u.setPhone(p.phone().trim());u.setAddress(p.address());u.setGender(p.gender());u.setBirthDate(p.birthDate());
  return mapper.toResponse(users.save(u));
 }
 @PostMapping("/password") @Transactional public Object password(@Valid @RequestBody Password p) {
  var u=current.get();
  if(!encoder.matches(p.currentPassword(),u.getPassword())) throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
  if(!p.newPassword().equals(p.confirmPassword())) throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
  if(p.newPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new IllegalArgumentException("Mật khẩu quá dài");
  u.setPassword(encoder.encode(p.newPassword()));users.save(u);tokens.revokeAll(u.getId());return java.util.Map.of("message","Đã đổi mật khẩu. Vui lòng đăng nhập lại.");
 }
 @PostMapping("/avatar") @Transactional public Object avatar(@RequestParam("file") MultipartFile file) throws java.io.IOException {
  var u=current.get(); if(file.isEmpty() || file.getSize()>5*1024*1024) throw new IllegalArgumentException("Ảnh phải nhỏ hơn 5 MB");
  byte[] bytes=file.getBytes(); java.awt.image.BufferedImage image;
  try(var stream=ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(bytes))) {
   var readers=ImageIO.getImageReaders(stream);if(!readers.hasNext())throw new IllegalArgumentException("Chỉ nhận ảnh JPG hoặc PNG");
   var reader=readers.next();try {reader.setInput(stream);String format=reader.getFormatName();if(!format.equalsIgnoreCase("PNG") && !format.equalsIgnoreCase("JPEG"))throw new IllegalArgumentException("Chỉ nhận ảnh JPG hoặc PNG");
    if(reader.getWidth(0)>6000 || reader.getHeight(0)>6000)throw new IllegalArgumentException("Ảnh tối đa 6000 × 6000 pixel");image=reader.read(0);
   } finally {reader.dispose();}
  }
  int size=512;var resized=new java.awt.image.BufferedImage(size,size,java.awt.image.BufferedImage.TYPE_INT_RGB);var g=resized.createGraphics();g.setColor(java.awt.Color.WHITE);g.fillRect(0,0,size,size);g.drawImage(image,0,0,size,size,null);g.dispose();
  var folder=Path.of("uploads/avatars").toAbsolutePath().normalize();Files.createDirectories(folder);String name=java.util.UUID.randomUUID()+".jpg";ImageIO.write(resized,"jpg",folder.resolve(name).toFile());
  u.setAvatar("/uploads/avatars/"+name);return mapper.toResponse(users.save(u));
 }
}
