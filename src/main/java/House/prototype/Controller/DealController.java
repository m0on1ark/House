package House.prototype.Controller;

import House.prototype.Model.Deal;
import House.prototype.Model.User;
import House.prototype.Service.DealService;
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
public class DealController {
    private final DealService dealService;
    public DealController(DealService dealService){
        this.dealService = dealService;
    }

    @GetMapping("/deals")
    public String findDeals(@RequestParam(required = false) Double radius,
                            HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        if (radius != null) {
            session.setAttribute("dealRadius", radius);
        }
        Object stored = session.getAttribute("dealRadius");
        double effectiveRadius = stored != null ? ((Number) stored).doubleValue() : 0.0;
        model.addAttribute("deals", dealService.findForUser(user.getId(), effectiveRadius));
        model.addAttribute("radius", effectiveRadius);
        model.addAttribute("homeBuildingId", dealService.homeBuildingId(user.getId()));
        House.prototype.Model.Building home = dealService.homeBuilding(user.getId());
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
                             HttpSession session){
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        try{
            byte[] imageBytes = null;
            String imageType = null;
            if (image != null && !image.isEmpty()) {
                imageBytes = image.getBytes();
                imageType = image.getContentType();
            }
            dealService.createDeal(user.getId(), productName, storeName, totalUnits, authorUnits,
                    pricePerUnit, imageBytes, imageType);
        }catch (IllegalArgumentException | IOException e){
            return "redirect:/deals?error";
        }
        return "redirect:/deals";
    }

    @GetMapping("/deals/{id}/image")
    public ResponseEntity<byte[]> dealImage(@PathVariable long id){
        Deal deal = dealService.getById(id);
        if (!deal.hasImage()) {
            return ResponseEntity.notFound().build();
        }
        MediaType type = deal.getImageType() != null
                ? MediaType.parseMediaType(deal.getImageType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().contentType(type).body(deal.getImage());
    }
    @PostMapping("/deals/{id}/delete")
    public String deleteDeal(@PathVariable long id, HttpSession session){
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        try{
            dealService.deleteDeal(id, user.getId());
        }catch (IllegalArgumentException e){
            return "redirect:/deals?error";
        }
        return "redirect:/deals";
    }

    @PostMapping("/deals/{id}/claim/cancel")
    public String cancelClaim(@PathVariable long id, HttpSession session){
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        try{
            dealService.cancelClaim(id, user.getId());
        }catch (IllegalArgumentException e){
            return "redirect:/deals?error";
        }
        return "redirect:/deals";
    }

    @PostMapping("/deals/{id}/claim")
    public String claimDeal(@PathVariable long id, @RequestParam int units, HttpSession session){
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        try{
            dealService.claim(id, user.getId(), units);
        }catch (IllegalArgumentException e){
            return "redirect:/deals?error";
        }
        return "redirect:/deals";
    }

}
