package House.prototype.Controller;

import House.prototype.Model.BuildingMessage;
import House.prototype.Model.DealMessage;
import House.prototype.Model.User;
import House.prototype.Service.ChatService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/deals/{id}/chat")
    public String showDealChat(@PathVariable long id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        try {
            model.addAttribute("deal", chatService.dealFor(id, user.getId()));
            model.addAttribute("messages", chatService.dealMessages(id, user.getId()));
        } catch (IllegalArgumentException e) {
            return "redirect:/deals?error";
        }
        return "deal-chat";
    }

    @PostMapping("/deals/{id}/chat")
    public String sendDealMessage(@PathVariable long id,
                                  @RequestParam(required = false) String text,
                                  @RequestParam(required = false) MultipartFile image,
                                  HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        try {
            byte[] bytes = null;
            String type = null;
            if (image != null && !image.isEmpty()) {
                bytes = image.getBytes();
                type = image.getContentType();
            }
            chatService.sendDealMessage(id, user.getId(), text, bytes, type);
        } catch (IllegalArgumentException | IOException e) {
            return "redirect:/deals?error";
        }
        return "redirect:/deals/" + id + "/chat";
    }

    @GetMapping("/chat/deal/image/{messageId}")
    public ResponseEntity<byte[]> dealMessageImage(@PathVariable long messageId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            DealMessage m = chatService.dealMessage(messageId, user.getId());
            if (!m.hasImage()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok().contentType(mediaType(m.getImageType())).body(m.getImage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/building/chat")
    public String showBuildingChat(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        try {
            model.addAttribute("building", chatService.buildingFor(user.getId()));
            model.addAttribute("messages", chatService.buildingMessages(user.getId()));
        } catch (IllegalArgumentException e) {
            return "redirect:/account?error";
        }
        return "building-chat";
    }

    @PostMapping("/building/chat")
    public String sendBuildingMessage(@RequestParam(required = false) String text,
                                      @RequestParam(required = false) MultipartFile image,
                                      HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        try {
            byte[] bytes = null;
            String type = null;
            if (image != null && !image.isEmpty()) {
                bytes = image.getBytes();
                type = image.getContentType();
            }
            chatService.sendBuildingMessage(user.getId(), text, bytes, type);
        } catch (IllegalArgumentException | IOException e) {
            return "redirect:/account?error";
        }
        return "redirect:/building/chat";
    }

    @GetMapping("/chat/building/image/{messageId}")
    public ResponseEntity<byte[]> buildingMessageImage(@PathVariable long messageId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            BuildingMessage m = chatService.buildingMessage(messageId, user.getId());
            if (!m.hasImage()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok().contentType(mediaType(m.getImageType())).body(m.getImage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private static MediaType mediaType(String type) {
        return type != null ? MediaType.parseMediaType(type) : MediaType.APPLICATION_OCTET_STREAM;
    }
}
