from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.encoders import jsonable_encoder
from sqlalchemy.orm import Session
from typing import List, Optional
from app import schemas, crud, models
import app.crud

from app.database import get_db
from app.utils.circuit_breaker import CircuitBreaker
from app.dependencies import get_api_key, verify_token
from app.custom_logging import logger

router = APIRouter(dependencies=[Depends(get_api_key), Depends(verify_token)])
cb=CircuitBreaker(failure_threshold=3, recovery_timeout=30)

@router.post(
    "/",
    response_model=schemas.ProductResponse,
    status_code=status.HTTP_201_CREATED  
)
async def create_product(
    product: schemas.ProductCreate, 
    db: Session = Depends(get_db)
):
    try:
        db_product = crud.create_product(db=db, product=product)
        logger.info("Product created successfully")
        return {"data": db_product}
    except Exception as e:
        logger.error(f"Create product error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Could not create product"
        )

@router.get(
    "/",
    response_model=schemas.ProductListResponse,
    status_code=status.HTTP_200_OK
)
async def read_products(
    skip:int =0,
    limit:int=100,
    category:Optional[str]=None,
    db:Session=Depends(get_db)
):
    try:
        products=crud.get_products(
            db, 
            skip=skip, 
            limit=limit, 
            category=category
        )
        
        logger.info("Fetching products")
        return {
            "data": products,
            "count": len(products)
        }
    except Exception as e:
        logger.error(f"Get products error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Could not fetch products"
        )
    
      
@router.get(
    "/{product_id}", 
    response_model=schemas.ProductResponse,
    status_code=status.HTTP_200_OK
)
async def read_product(product_id: int, db: Session = Depends(get_db)):
    try:
        async with cb.protect():
            product = crud.get_product(db, product_id=product_id)
            if product is None:
                logger.warning("Product not found")
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail="Product not found"
                )
            
            logger.info("Fetched product")
            return {"data": product}
    except Exception as e:
        logger.error(f"Get product error: {str(e)}")
        raise


@router.put("/{product_id}", 
            response_model=schemas.ProductResponse,
            status_code=status.HTTP_200_OK)
def update_product(
    product_id: int, 
    product: schemas.ProductUpdate, 
    db: Session = Depends(get_db)
):    
    try:
        db_product = crud.get_product(db, product_id=product_id)
        logger.info("Product updated successfully")
        
        if db_product is None:
            raise HTTPException(status_code=404, detail="Product not found")
    
        return crud.update_product(db=db, product_id=product_id, product=product)
    except Exception as e:
        logger.error(f"Uodate product error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Could not update product"
        )

@router.delete("/{product_id}", 
               response_model=schemas.Product,
               status_code=status.HTTP_200_OK)
def delete_product(product_id: int, db: Session = Depends(get_db)):
    try:
        db_product = crud.get_product(db, product_id=product_id)
        if db_product is None:
            raise HTTPException(status_code=404, detail="Product not found")
        return crud.delete_product(db=db, product_id=product_id)
    except Exception as e:
        logger.error(f"Delete product error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Could not delete product"
        )