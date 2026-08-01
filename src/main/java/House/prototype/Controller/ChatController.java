package House.prototype.Controller;

import House.prototype.Exception.InvalidRequestException;
import House.prototype.Model.BuildingMessage;
import House.prototype.Model.DealMessage;
import House.prototype.Security.AppUserDetails;
import House.prototype.Service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public String showDealChat(@PathVariable long id,
                               @AuthenticationPrincipal AppUserDetails principal, Model model) {
        try {
            model.addAttribute("deal", chatService.dealFor(id, principal.getId()));
            model.addAttribute("messages", chatService.dealMessages(id, principal.getId()));
        } catch (InvalidRequestException e) {
            return "redirect:/deals?error";
        }
        return "deal-chat";
    }

    @PostMapping("/deals/{id}/chat")
    public String sendDealMessage(@PathVariable long id,
                                  @RequestParam(required = false) String text,
                                  @RequestParam(required = false) MultipartFile image,
                                  @AuthenticationPrincipal AppUserDetails principal) {
        try {
            byte[] bytes = null;
            String type = null;
            if (image != null && !image.isEmpty()) {
                bytes = image.getBytes();
                type = image.getContentType();
            }
            chatService.sendDealMessage(id, principal.getId(), text, bytes, type);
        } catch (InvalidRequestException | IOException e) {
            return "redirect:/deals?error";
        }
        return "redirect:/deals/" + id + "/chat";
    }

    @GetMapping("/chat/deal/image/{messageId}")
    public ResponseEntity<byte[]> dealMessageImage(@PathVariable long messageId,
                                                   @AuthenticationPrincipal AppUserDetails principal) {
        try {
            DealMessage m = chatService.dealMessage(messageId, principal.getId());
            if (!m.hasImage()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok().contentType(mediaType(m.getImageType())).body(m.getImage());
        } catch (InvalidRequestException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/building/chat")
    public String showBuildingChat(@AuthenticationPrincipal AppUserDetails principal, Model model) {
        try {
            model.addAttribute("building", chatService.buildingFor(principal.getId()));
            model.addAttribute("messages", chatService.buildingMessages(principal.getId()));
        } catch (InvalidRequestException e) {
            return "redirect:/account?error";
        }
        return "building-chat";
    }

    @PostMapping("/building/chat")
    public String sendBuildingMessage(@RequestParam(required = false) String text,
                                      @RequestParam(required = false) MultipartFile image,
                                      @AuthenticationPrincipal AppUserDetails principal) {
        try {
            byte[] bytes = null;
            String type = null;
            if (image != null && !image.isEmpty()) {
                bytes = image.getBytes();
                type = image.getContentType();
            }
            chatService.sendBuildingMessage(principal.getId(), text, bytes, type);
        } catch (InvalidRequestException | IOException e) {
            return "redirect:/account?error";
        }
        return "redirect:/building/chat";
    }

    @GetMapping("/chat/building/image/{messageId}")
    public ResponseEntity<byte[]> buildingMessageImage(@PathVariable long messageId,
                                                       @AuthenticationPrincipal AppUserDetails principal) {
        try {
            BuildingMessage m = chatService.buildingMessage(messageId, principal.getId());
            if (!m.hasImage()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok().contentType(mediaType(m.getImageType())).body(m.getImage());
        } catch (InvalidRequestException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private static MediaType mediaType(String type) {
        return type != null ? MediaType.parseMediaType(type) : MediaType.APPLICATION_OCTET_STREAM;
    }
}
