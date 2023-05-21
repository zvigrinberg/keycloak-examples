package com.redhat.zgrinber.keycloakintegrationspringboot.api;

import com.redhat.zgrinber.keycloakintegrationspringboot.exception.DuplicateEntityFoundException;
import com.redhat.zgrinber.keycloakintegrationspringboot.exception.EntityNotFoundException;
import com.redhat.zgrinber.keycloakintegrationspringboot.model.Item;
import com.redhat.zgrinber.keycloakintegrationspringboot.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("items")
@RequiredArgsConstructor
public class ItemResource {

    private final ItemService itemService;
    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemService(@PathVariable String id)
    {
        Item oneItem = null;
        ResponseEntity response;
        try {
            oneItem = itemService.getOneItem(id);
            response = ResponseEntity.ok(oneItem);
        } catch (EntityNotFoundException e) {
            response = ResponseEntity.notFound().header("errorMessage",e.toString()).build();
        }
        return response;
    }

    @GetMapping
    public List<Item> getAll()
    {
        return itemService.getAll();
    }

    @PostMapping
    public ResponseEntity createItem(@RequestBody Item item, @RequestHeader("host") String host)
    {
        try {
            itemService.createItem(item);
        } catch (DuplicateEntityFoundException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.toString());
        }
        return ResponseEntity.created(URI.create("http://" + host + "/items/" + item.getId())).body("Item Created");
    }

    @PutMapping
    public ResponseEntity updateItem(@RequestBody Item item)
    {
        try {
            itemService.updateItem(item);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.noContent().header("errorMessage",e.toString()).build();
        }
        return ResponseEntity.ok(("Item Updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity deleteItem(@PathVariable String id)
    {
        try {
            itemService.deleteItem(id);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.noContent().header("errorMessage",e.toString()).build();
        }
        return ResponseEntity.ok(("Item with id=" + id +" was deleted"));
    }




}
