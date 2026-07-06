package House.prototype.Controller;

import House.prototype.DTO.BuildingSuggestion;
import House.prototype.Service.BuildingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BuildingController {
    private final BuildingService buildingService;
    public BuildingController(BuildingService buildingService){
        this.buildingService = buildingService;
    }
    @GetMapping("/api/buildings/search")
    public List<BuildingSuggestion> suggest(@RequestParam String q){
        return buildingService.findByPrefix(q);
    }
}
