from sqlalchemy.orm import Session
from typing import List, Optional
from app import models, schemas
from app.custom_logging import logger

def get_product(db: Session, product_id: int) -> Optional[models.Product]:
    return db.query(models.Product).filter(models.Product.id==product_id).first()

def get_products(
        db:Session,
        skip:int=0,
        limit:int=100,
        category:Optional[str]=None,
        active_only:bool=True,
    ) -> List[models.Product]:

    query=db.query(models.Product)
    if active_only:
        query=query.filter(models.Product.is_active == True)
    if category:
        query=query.filter(models.Product.category == category)
        

    return query.offset(skip).limit(limit).all()


def create_product(db:Session, product: schemas.ProductCreate) -> models.Product:
    db_product=models.Product(**product.dict())
    db.add(db_product)
    db.commit()
    db.refresh(db_product)
    logger.info(f"Created product {db_product.id}")
    return db_product

def update_product(
            db:Session,
            product_id:int,
        product: schemas.ProductUpdate)-> Optional[models.Product]:
        db_product = get_product(db, product_id)
        if not db_product:
            return None
        
        update_data = product.dict(exclude_unset=True)
        for field, value in update_data.items():
          setattr(db_product, field, value)
    
        db.commit()
        db.refresh(db_product)
        logger.info(f"Updated product {product_id}")
        
        return db_product

def delete_product(db: Session, product_id: int) -> schemas.ProductDeleteResponse:
    db_product = get_product(db, product_id)
    delResponse = schemas.ProductDeleteResponse()
    delResponse.id =  product_id
    if not db_product:
        delResponse.success = False
        return delResponse
    
    db.delete(db_product)
    db.commit()
    logger.info(f"Deleted product {product_id}")
    
    delResponse.success = True
    return delResponse

def reserve_product_stock(db: Session, product_id: int, quantity: int) -> bool:
    product = get_product(db, product_id)
    if not product or product.stock < quantity:
        return False
    
    product.stock -= quantity
    db.commit()
    logger.info(f"Reserved {quantity} of product {product_id}")
    return True
