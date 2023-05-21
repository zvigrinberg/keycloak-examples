package com.redhat.zgrinber.keycloakintegrationspringboot.service;

import com.redhat.zgrinber.keycloakintegrationspringboot.exception.DuplicateEntityFoundException;
import com.redhat.zgrinber.keycloakintegrationspringboot.exception.EntityNotFoundException;
import com.redhat.zgrinber.keycloakintegrationspringboot.model.Customer;
import com.redhat.zgrinber.keycloakintegrationspringboot.model.Item;

import java.util.List;

public interface ItemService {

    Item getOneItem(String id) throws EntityNotFoundException;
    List<Item> getAll();

    void createItem(Item item) throws DuplicateEntityFoundException;

    void deleteItem(String id) throws EntityNotFoundException;

    void updateItem(Item item) throws EntityNotFoundException;


}
