from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List, Optional

from app import crud
from .. import schemas, models, database
from app.dependencies import get_api_key, verify_token
from app.custom_logging import logger

router = APIRouter(dependencies=[Depends(get_api_key), Depends(verify_token)])

@router.post("/", 
             response_model=schemas.Order,
             status_code=status.HTTP_201_CREATED)
def create_order(order: schemas.OrderCreate, db: Session = Depends(database.get_db)):
    try:
        db_order = models.Order(
            user_id=order.user_id,
            status="pending",
            total_amount=sum(item.unit_price * item.quantity for item in order.items)
        )
        db.add(db_order)
        db.commit()
        db.refresh(db_order)
        
        for item in order.items:
            db_item = models.OrderItem(
                order_id=db_order.id,
                product_id=item.product_id,
                quantity=item.quantity,
                unit_price=item.unit_price
            )
            db.add(db_item)
        
        db.commit()
        db.refresh(db_order)

        logger.info("Order created successfully")

        return db_order
    except Exception as e:
        logger.error(f"Create order error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Could not create order"
        )       

@router.get("/{order_id}", response_model=schemas.Order)
def read_order(order_id: int, db: Session = Depends(database.get_db)):
    try:
        order = db.query(models.Order).filter(models.Order.id == order_id).first()
        if not order:
            logger.warning("Order not found")
            raise HTTPException(status_code=404, detail="Order not found")
        
        logger.info("Fetched order")
        return order
    except Exception as e:
        logger.error(f"Get orders error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Could not fetch order")

@router.get(
    "/",
    response_model=schemas.OrderListResponse
)
async def read_orders(
    skip:int =0,
    limit:int=100,
    db:Session=Depends(database.get_db)
):
    try:
        orders=crud.get_orders(
            db, 
            skip=skip, 
            limit=limit
        )
        
        logger.info("Fetching orders")
        return {
            "data": orders,
            "count": len(orders)
        }
    except Exception as e:
        logger.error(f"Get orders error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Could not fetch orders")