package com.redhat.zgrinber.keycloakintegrationspringboot.service;

import com.redhat.zgrinber.keycloakintegrationspringboot.exception.DuplicateEntityFoundException;
import com.redhat.zgrinber.keycloakintegrationspringboot.exception.EntityNotFoundException;
import com.redhat.zgrinber.keycloakintegrationspringboot.model.Customer;
import com.redhat.zgrinber.keycloakintegrationspringboot.model.Item;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class InMemoryItemService implements ItemService {

    private static final Map<String, Item> items;
    static
    {
        items = new HashMap();
        Item item = new Item("demo-item", "Super Item", "unnatural", 1000);
        items.put("demo-item",item);
    }
    @Override
    public Item getOneItem(String id) throws EntityNotFoundException {
        return Optional.ofNullable(items.get(id)).orElseThrow(() ->  new EntityNotFoundException("Item with id=" + id + " wasn't found in DB" ,"Item"));
    }

    @Override
    public List<Item> getAll() {
        return items.values().stream().toList();
    }

    @Override
    public void createItem(Item item) throws DuplicateEntityFoundException {
        if(items.get(item.getId()) == null) {
            items.put(item.getId(), item);
        }
        else
        {
            throw new DuplicateEntityFoundException("Item with id=" + item.getId() + " is already exists in DB, aborting creation of item" ,"Item");
        }

    }

    @Override
    public void deleteItem(String id) throws EntityNotFoundException {
        try {
            Item itemToBeDeleted = this.getOneItem(id);
            items.remove(itemToBeDeleted.getId());
        } catch (EntityNotFoundException e) {
            throw new EntityNotFoundException("Item with id=" + id + " wasn't found in DB, Delete aborted" ,"Item");
        }

    }

    @Override
    public void updateItem(Item item) throws EntityNotFoundException {
        try {
            Item itemToBeUpdated = this.getOneItem(item.getId());
            itemToBeUpdated.setName(item.getName());
            itemToBeUpdated.setPrice(item.getPrice());
            itemToBeUpdated.setType(item.getType());
        } catch (EntityNotFoundException e) {
            throw new EntityNotFoundException("Item with id=" + item.getId() + " wasn't found in DB, Update aborted" ,"Item");

        }

    }
}
