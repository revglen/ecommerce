import sqlalchemy
from sqlalchemy.orm import Session
from typing import List, Optional
from app import models, schemas
from app.custom_logging import logger

def get_orders(
        db:Session,
        skip:int=0,
        limit:int=100,
        active_only:bool=True,
    ) -> List[models.Order]:

    orders = db.query(models.Order)\
        .offset(skip)\
        .limit(limit)\
        .options(sqlalchemy.orm.joinedload(models.Order.items))\
        .all()
    
    return orders

def get_order( db:Session, order_id:int ) -> schemas.Order:
    order = db.query(models.Order).filter(models.Order.id == order_id).first()
    return order

def delete_order(db: Session, order_id: int) -> schemas.OrderDeleteResponse:
    db_order = get_order(db, order_id)
    delResponse = schemas.OrderDeleteResponse()
    delResponse.id =  order_id
    if not db_order:
        delResponse.success = False
        return delResponse
  
    db.delete(db_order)
    db.commit()    
    logger.info(f"Deleted order {order_id}")
    
    delResponse.success = True
    return delResponse

def update_order(
            db:Session,
            order_id:int,
        order: schemas.OrderUpdate)-> Optional[schemas.OrderUpdateResponse]:
        db_order = get_order(db, order_id)
        if not db_order:
            return None
        
        update_data = order.dict(exclude_unset=True)        
        for field, value in update_data.items():
            print(f"\n{field}:{value}",field,value)
            print ("\n" + str(type(value)))
            if field != "items":
                setattr(db_order, field, value)
        
        if order.items is not None:
            # Delete existing items
            db.query(models.OrderItem).filter(models.OrderItem.order_id == order_id).delete()
            
            # Add new items
            for item in order.items:
                db_item = models.OrderItem(
                    order_id=order_id,
                    product_id=item.product_id,
                    quantity=item.quantity,
                    unit_price=item.unit_price
                )
                db.add(db_item)
        
        db.commit()
        db.refresh(db_order)

        orderUpdateResp = schemas.OrderUpdateResponse(
            id=order_id,
            **{k: v for k, v in order.dict().items() 
                if k in schemas.OrderUpdateResponse.__fields__})
        
        return orderUpdateResp