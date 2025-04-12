import sqlalchemy
from sqlalchemy.orm import Session
from typing import List, Optional
from app import models, schemas

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
