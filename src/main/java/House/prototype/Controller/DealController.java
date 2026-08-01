package House.prototype.Controller;

import House.prototype.Exception.InvalidRequestException;
import House.prototype.Model.Deal;
import House.prototype.Security.AppUserDetails;
import House.prototype.Service.DealService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
public class DealController {
    private final DealService dealService;
    public DealController(DealService dealService){
        this.dealService = dealService;
    }

    @GetMapping("/deals")
    public String findDeals(@RequestParam(required = false) Double radius,
                            @AuthenticationPrincipal AppUserDetails principal,
                            HttpSession session, Model model) {
        if (radius != null) {
            session.setAttribute("dealRadius", radius);
        }
        Object stored = session.getAttribute("dealRadius");
        double effectiveRadius = stored != null ? ((Number) stored).doubleValue() : 0.0;
        model.addAttribute("deals", dealService.findForUser(principal.getId(), effectiveRadius));
        model.addAttribute("radius", effectiveRadius);
        model.addAttribute("homeBuildingId", dealService.homeBuildingId(principal.getId()));
        House.prototype.Model.Building home = dealService.homeBuilding(principal.getId());
        if (home != null) {
            model.addAttribute("homeLat", home.getLat());
            model.addAttribute("homeLon", home.getLon());
        }
        return "deals";
    }
    @PostMapping("/deals")
    public String createDeal(@RequestParam String productName,
                             @RequestParam String storeName,
                             @RequestParam int totalUnits,
                             @RequestParam int authorUnits,
                             @RequestParam(required = false) Double pricePerUnit,
                             @RequestParam(required = false) MultipartFile image,
                             @AuthenticationPrincipal AppUserDetails principal) throws IOException {
        byte[] imageBytes = null;
        String imageType = null;
        if (image != null && !image.isEmpty()) {
            imageBytes = image.getBytes();
            imageType = image.getContentType();
        }
        dealService.createDeal(principal.getId(), productName, storeName, totalUnits, authorUnits,
                pricePerUnit, imageBytes, imageType);
        return "redirect:/deals";
    }

    @GetMapping("/deals/{id}/image")
    public ResponseEntity<byte[]> dealImage(@PathVariable long id){
        Deal deal;
        try{
            deal = dealService.getById(id);
        }catch (InvalidRequestException e){
            return ResponseEntity.notFound().build();
        }
        if (!deal.hasImage()) {
            return ResponseEntity.notFound().build();
        }
        MediaType type = deal.getImageType() != null
                ? MediaType.parseMediaType(deal.getImageType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().contentType(type).body(deal.getImage());
    }
    @PostMapping("/deals/{id}/delete")
    public String deleteDeal(@PathVariable long id,
                             @AuthenticationPrincipal AppUserDetails principal){
        dealService.deleteDeal(id, principal.getId());
        return "redirect:/deals";
    }

    @PostMapping("/deals/{id}/claim/cancel")
    public String cancelClaim(@PathVariable long id,
                              @AuthenticationPrincipal AppUserDetails principal){
        dealService.cancelClaim(id, principal.getId());
        return "redirect:/deals";
    }

    @PostMapping("/deals/{id}/claim")
    public String claimDeal(@PathVariable long id, @RequestParam int units,
                            @AuthenticationPrincipal AppUserDetails principal){
        dealService.claim(id, principal.getId(), units);
        return "redirect:/deals";
    }

    @ExceptionHandler({InvalidRequestException.class, IOException.class})
    public String handleInvalidRequest(){
        return "redirect:/deals?error";
    }

}
