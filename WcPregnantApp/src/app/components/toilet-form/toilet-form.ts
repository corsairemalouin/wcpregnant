import { Component, Input } from '@angular/core';

@Component({
  selector: 'wc-toilet-form',
  imports: [],
  templateUrl: './toilet-form.html',
  styleUrl: './toilet-form.css'
})
export class ToiletFormComponent {

@Input() location: any;

}
