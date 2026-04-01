import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ToiletDetails } from './toilet-details';

describe('ToiletDetails', () => {
  let component: ToiletDetails;
  let fixture: ComponentFixture<ToiletDetails>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToiletDetails]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ToiletDetails);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
